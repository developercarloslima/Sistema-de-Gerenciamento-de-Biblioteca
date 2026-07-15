package com.carlos.library.dto;

import com.carlos.library.domain.enums.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record RegisterRequest(@NotBlank @Size(max=120) String name, @Email @NotBlank String email,
                                  @NotBlank @Size(min=8,max=72) String password, @Size(max=30) String phone) {}
    public record TokenResponse(String tokenType, String accessToken, String refreshToken, long expiresIn) {}

    public record CreateUserRequest(@NotBlank String name, @Email @NotBlank String email,
                                    @NotBlank @Size(min=8,max=72) String password, @NotNull Role role) {}
    public record UserResponse(UUID id, String name, String email, Role role, boolean active, OffsetDateTime createdAt) {}
    public record UserStatusRequest(@NotNull Boolean active) {}
    public record ProfileResponse(UUID userId, UUID memberId, String name, String email, Role role,
                                  boolean active, MemberStatus memberStatus, String registrationNumber) {}

    public record BookRequest(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=20) String isbn,
                              @NotBlank @Size(max=160) String author, @Size(max=120) String publisher,
                              @Min(0) Integer publicationYear, @NotBlank @Size(max=80) String category,
                              @Size(max=5000) String description) {}
    public record BookResponse(UUID id, String title, String isbn, String author, String publisher,
                               Integer publicationYear, String category, String description, boolean active,
                               long totalCopies, long availableCopies, OffsetDateTime createdAt) {}
    public record BookCopyRequest(@NotBlank @Size(max=40) String inventoryCode, @NotNull LocalDate acquisitionDate) {}
    public record BookCopyStatusRequest(@NotNull BookCopyStatus status) {}
    public record BookCopyResponse(UUID id, UUID bookId, String bookTitle, String inventoryCode,
                                   BookCopyStatus status, LocalDate acquisitionDate) {}

    public record MemberCreateRequest(@NotBlank String name, @Email @NotBlank String email,
                                      @NotBlank @Size(min=8,max=72) String password, @Size(max=30) String phone,
                                      @Min(1) @Max(20) Integer maximumLoans) {}
    public record MemberStatusRequest(@NotNull MemberStatus status) {}
    public record MemberUpdateRequest(@NotBlank @Size(max=120) String name, @Size(max=30) String phone,
                                      @Min(1) @Max(20) int maximumLoans) {}
    public record MemberResponse(UUID id, UUID userId, String name, String email, String registrationNumber,
                                 String phone, MemberStatus status, int maximumLoans, OffsetDateTime createdAt) {}

    public record LoanCreateRequest(@NotNull UUID memberId, @NotNull UUID bookCopyId, Integer loanDays) {}
    public record LoanResponse(UUID id, UUID memberId, String memberName, UUID bookCopyId, String inventoryCode,
                               UUID bookId, String bookTitle, LocalDate loanDate, LocalDate dueDate,
                               LocalDate returnDate, int renewalCount, LoanStatus status, String createdBy) {}
    public record ReturnResponse(LoanResponse loan, BigDecimal generatedFine) {}

    public record ReservationCreateRequest(UUID memberId, @NotNull UUID bookId) {}
    public record ReservationResponse(UUID id, UUID memberId, String memberName, UUID bookId, String bookTitle,
                                      UUID bookCopyId, OffsetDateTime reservationDate, OffsetDateTime expirationDate,
                                      ReservationStatus status) {}

    public record FineResponse(UUID id, UUID memberId, String memberName, UUID loanId, String bookTitle,
                               BigDecimal amount, String reason, FineStatus status, OffsetDateTime paidAt,
                               OffsetDateTime createdAt) {}

    public record NotificationResponse(UUID id, String title, String message, NotificationType type,
                                       boolean read, UUID referenceId, OffsetDateTime createdAt) {}

    public record MessageResponse(String message) {}
}
