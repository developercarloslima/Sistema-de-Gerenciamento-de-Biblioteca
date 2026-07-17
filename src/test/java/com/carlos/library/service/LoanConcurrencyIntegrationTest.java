package com.carlos.library.service;

import com.carlos.library.domain.entity.Book;
import com.carlos.library.domain.entity.BookCopy;
import com.carlos.library.domain.entity.Member;
import com.carlos.library.domain.entity.UserAccount;
import com.carlos.library.domain.enums.BookCopyStatus;
import com.carlos.library.domain.enums.LoanStatus;
import com.carlos.library.domain.enums.MemberStatus;
import com.carlos.library.domain.enums.Role;
import com.carlos.library.dto.ApiDtos.LoanCreateRequest;
import com.carlos.library.dto.ApiDtos.LoanResponse;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.repository.BookCopyRepository;
import com.carlos.library.repository.BookRepository;
import com.carlos.library.repository.LoanRepository;
import com.carlos.library.repository.MemberRepository;
import com.carlos.library.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.storage.r2.enabled=false",
        "app.scheduler.deadlines-cron=-"
})
@ActiveProfiles("integration")
@Testcontainers(disabledWithoutDocker = true)
class LoanConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private LoanService loanService;
    @Autowired private LoanRepository loans;
    @Autowired private BookRepository books;
    @Autowired private BookCopyRepository copies;
    @Autowired private UserAccountRepository users;
    @Autowired private MemberRepository members;
    @Autowired private JdbcTemplate jdbc;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbc.execute("""
                TRUNCATE TABLE
                    notifications,
                    fines,
                    reservations,
                    loans,
                    reading_progress,
                    book_upload_sessions,
                    book_assets,
                    book_copies,
                    books,
                    members,
                    app_users
                RESTART IDENTITY CASCADE
                """);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void pessimisticLockAllowsOnlyOneConcurrentLoanForTheSameCopy() throws Exception {
        Member firstMember = createMember("first@library.test", "MBR-LOCK-01");
        Member secondMember = createMember("second@library.test", "MBR-LOCK-02");
        BookCopy copy = createAvailableCopy();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Attempt> firstAttempt = executor.submit(
                () -> attemptLoan(firstMember.getId(), copy.getId(), ready, start)
        );
        Future<Attempt> secondAttempt = executor.submit(
                () -> attemptLoan(secondMember.getId(), copy.getId(), ready, start)
        );

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Attempt> attempts = List.of(
                firstAttempt.get(15, TimeUnit.SECONDS),
                secondAttempt.get(15, TimeUnit.SECONDS)
        );

        assertThat(attempts).filteredOn(Attempt::successful).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> !attempt.successful()).hasSize(1);
        assertThat(attempts)
                .filteredOn(attempt -> !attempt.successful())
                .first()
                .extracting(Attempt::error)
                .isInstanceOf(BusinessException.class);

        assertThat(loans.countByBookCopyIdAndStatusIn(
                copy.getId(),
                List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE)
        )).isEqualTo(1);

        BookCopy persistedCopy = copies.findById(copy.getId()).orElseThrow();
        assertThat(persistedCopy.getStatus()).isEqualTo(BookCopyStatus.LOANED);
    }

    @Test
    void databaseConstraintRejectsASecondActiveLoanForTheSameCopy() {
        Member firstMember = createMember("db-first@library.test", "MBR-DB-01");
        Member secondMember = createMember("db-second@library.test", "MBR-DB-02");
        BookCopy copy = createAvailableCopy();

        insertLoan(UUID.randomUUID(), firstMember.getId(), copy.getId(), "ACTIVE");

        assertThatThrownBy(() ->
                insertLoan(UUID.randomUUID(), secondMember.getId(), copy.getId(), "OVERDUE")
        ).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(loans.countByBookCopyIdAndStatusIn(
                copy.getId(),
                List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE)
        )).isEqualTo(1);
    }

    private Attempt attemptLoan(
            UUID memberId,
            UUID copyId,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return Attempt.failure(new IllegalStateException("Tempo de espera excedido."));
            }
            LoanResponse response = loanService.create(
                    new LoanCreateRequest(memberId, copyId, 14),
                    "integration-test@library.test"
            );
            return Attempt.success(response.id());
        } catch (Exception exception) {
            return Attempt.failure(exception);
        }
    }

    private Member createMember(String email, String registrationNumber) {
        UserAccount user = new UserAccount();
        user.setName(registrationNumber);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(Role.MEMBER);
        user.setActive(true);
        users.saveAndFlush(user);

        Member member = new Member();
        member.setUser(user);
        member.setRegistrationNumber(registrationNumber);
        member.setStatus(MemberStatus.ACTIVE);
        member.setMaximumLoans(3);
        return members.saveAndFlush(member);
    }

    private BookCopy createAvailableCopy() {
        Book book = new Book();
        book.setTitle("Concorrência em Sistemas de Biblioteca");
        book.setIsbn("ISBN-" + UUID.randomUUID().toString().substring(0, 12));
        book.setAuthor("Carlos Lima");
        book.setCategory("Engenharia de Software");
        book.setActive(true);
        books.saveAndFlush(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setInventoryCode("COPY-" + UUID.randomUUID().toString().substring(0, 8));
        copy.setStatus(BookCopyStatus.AVAILABLE);
        copy.setAcquisitionDate(LocalDate.now());
        return copies.saveAndFlush(copy);
    }

    private void insertLoan(UUID loanId, UUID memberId, UUID copyId, String status) {
        jdbc.update("""
                INSERT INTO loans (
                    id,
                    member_id,
                    book_copy_id,
                    loan_date,
                    due_date,
                    return_date,
                    renewal_count,
                    status,
                    created_by,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, CURRENT_DATE, CURRENT_DATE + 14, NULL, 0, ?, ?, NOW(), NOW())
                """,
                loanId,
                memberId,
                copyId,
                status,
                "integration-test@library.test"
        );
    }

    private record Attempt(boolean successful, UUID loanId, Exception error) {
        static Attempt success(UUID loanId) {
            return new Attempt(true, loanId, null);
        }

        static Attempt failure(Exception error) {
            return new Attempt(false, null, error);
        }
    }
}
