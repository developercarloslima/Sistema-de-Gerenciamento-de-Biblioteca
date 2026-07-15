package com.carlos.library.service;

import com.carlos.library.domain.entity.Notification;
import com.carlos.library.domain.entity.UserAccount;
import com.carlos.library.domain.enums.NotificationType;
import com.carlos.library.dto.ApiDtos.NotificationResponse;
import com.carlos.library.exception.NotFoundException;
import com.carlos.library.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final MapperService mapper;

    @Transactional
    public void createOnce(UserAccount user, String title, String message, NotificationType type, UUID referenceId) {
        if (referenceId != null && notifications.existsByUserIdAndTypeAndReferenceId(user.getId(), type, referenceId)) return;
        Notification n = new Notification();
        n.setUser(user); n.setTitle(title); n.setMessage(message); n.setType(type); n.setReferenceId(referenceId);
        notifications.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> mine(String email, Pageable pageable) {
        return notifications.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(email, pageable).map(mapper::notification);
    }

    @Transactional
    public NotificationResponse markRead(UUID id, String email) {
        Notification n = notifications.findById(id).orElseThrow(() -> new NotFoundException("Notificação não encontrada."));
        if (!n.getUser().getEmail().equalsIgnoreCase(email)) throw new NotFoundException("Notificação não encontrada.");
        n.setRead(true);
        return mapper.notification(n);
    }

    @Transactional
    public int markAllRead(String email) { return notifications.markAllRead(email); }
}
