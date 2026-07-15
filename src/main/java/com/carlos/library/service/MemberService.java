package com.carlos.library.service;

import com.carlos.library.domain.entity.Member;
import com.carlos.library.domain.entity.UserAccount;
import com.carlos.library.domain.enums.MemberStatus;
import com.carlos.library.domain.enums.Role;
import com.carlos.library.dto.ApiDtos.*;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.exception.NotFoundException;
import com.carlos.library.repository.MemberRepository;
import com.carlos.library.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository members;
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final MapperService mapper;

    @Transactional
    public MemberResponse create(MemberCreateRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) throw new BusinessException("E-mail já cadastrado.");
        UserAccount user = new UserAccount();
        user.setName(request.name().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPassword(encoder.encode(request.password()));
        user.setRole(Role.MEMBER);
        user.setActive(true);
        users.save(user);

        Member member = new Member();
        member.setUser(user);
        member.setPhone(request.phone());
        member.setMaximumLoans(request.maximumLoans() == null ? 3 : request.maximumLoans());
        member.setStatus(MemberStatus.ACTIVE);
        member.setRegistrationNumber(generateRegistration());
        return mapper.member(members.save(member));
    }

    @Transactional(readOnly = true)
    public Page<MemberResponse> list(Pageable pageable) {
        return members.findAll(pageable).map(m -> mapper.member(members.findWithUserById(m.getId()).orElseThrow()));
    }

    @Transactional(readOnly = true)
    public MemberResponse get(UUID id) { return mapper.member(find(id)); }

    @Transactional
    public MemberResponse update(UUID id, MemberUpdateRequest request) {
        Member m = find(id);
        m.getUser().setName(request.name().trim());
        m.setPhone(request.phone());
        m.setMaximumLoans(request.maximumLoans());
        return mapper.member(m);
    }

    @Transactional
    public MemberResponse updateStatus(UUID id, MemberStatusRequest request) {
        Member m = find(id);
        m.setStatus(request.status());
        m.getUser().setActive(request.status() != MemberStatus.INACTIVE);
        return mapper.member(m);
    }

    public Member find(UUID id) {
        return members.findWithUserById(id).orElseThrow(() -> new NotFoundException("Membro não encontrado."));
    }

    public Member findByEmail(String email) {
        return members.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Membro não encontrado para o usuário autenticado."));
    }

    private String generateRegistration() {
        String code;
        do { code = "MBR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT); }
        while (members.existsByRegistrationNumber(code));
        return code;
    }
}
