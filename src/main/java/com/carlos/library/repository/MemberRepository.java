package com.carlos.library.repository;

import com.carlos.library.domain.entity.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @EntityGraph(attributePaths = "user")
    Optional<Member> findWithUserById(UUID id);

    @EntityGraph(attributePaths = "user")
    Optional<Member> findByUserEmailIgnoreCase(String email);

    boolean existsByRegistrationNumber(String registrationNumber);
}
