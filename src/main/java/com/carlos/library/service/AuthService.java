package com.carlos.library.service;

import com.carlos.library.domain.entity.Member;
import com.carlos.library.domain.entity.UserAccount;
import com.carlos.library.domain.enums.MemberStatus;
import com.carlos.library.domain.enums.Role;
import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.repository.MemberRepository;
import com.carlos.library.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository users;
    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email().toLowerCase(Locale.ROOT), request.password()));
        UserAccount user = users.findByEmailIgnoreCase(request.email()).orElseThrow();
        return jwtService.issue(user);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Já existe um usuário com este e-mail.");
        }
        UserAccount user = createUser(request.name(), request.email(), request.password(), Role.MEMBER);
        Member member = new Member();
        member.setUser(user);
        member.setPhone(request.phone());
        member.setStatus(MemberStatus.ACTIVE);
        member.setMaximumLoans(3);
        member.setRegistrationNumber(generateRegistration());
        members.save(member);
        return jwtService.issue(user);
    }

    private UserAccount createUser(String name, String email, String password, Role role) {
        UserAccount user = new UserAccount();
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase(Locale.ROOT));
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        return users.save(user);
    }

    private String generateRegistration() {
        String code;
        do {
            code = "MBR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (members.existsByRegistrationNumber(code));
        return code;
    }
}
