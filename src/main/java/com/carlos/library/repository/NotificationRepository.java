package com.carlos.library.repository;

import com.carlos.library.domain.entity.Notification;
import com.carlos.library.domain.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    boolean existsByUserIdAndTypeAndReferenceId(UUID userId, NotificationType type, UUID referenceId);

    @EntityGraph(attributePaths = "user")
    Page<Notification> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String email, Pageable pageable);

    @Modifying
    @Query("update Notification n set n.read = true where n.user.email = :email and n.read = false")
    int markAllRead(@Param("email") String email);
}
