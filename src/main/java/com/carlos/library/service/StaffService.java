package com.carlos.library.service;

import com.carlos.library.domain.entity.UserAccount;
import com.carlos.library.domain.enums.Role;
import com.carlos.library.dto.ApiDtos.CreateUserRequest;
import com.carlos.library.dto.ApiDtos.UserResponse;
import com.carlos.library.dto.ApiDtos.UserStatusRequest;
import com.carlos.library.exception.NotFoundException;

import java.util.UUID;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StaffService {
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (request.role() == Role.MEMBER) {
            throw new BusinessException("Use o endpoint de membros para criar usuários MEMBER.");
        }
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("E-mail já cadastrado.");
        }
        UserAccount user = new UserAccount();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPassword(encoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);
        return map(users.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return users.findByRoleNot(Role.MEMBER, pageable).map(this::map);
    }

    @Transactional
    public UserResponse updateStatus(UUID id, UserStatusRequest request) {
        UserAccount user = users.findById(id)
                .filter(found -> found.getRole() != Role.MEMBER)
                .orElseThrow(() -> new NotFoundException("Funcionário não encontrado."));
        user.setActive(request.active());
        return map(user);
    }

    private UserResponse map(UserAccount user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.isActive(), user.getCreatedAt());
    }
}
