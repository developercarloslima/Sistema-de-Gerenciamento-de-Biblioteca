package com.carlos.library.service;

import com.carlos.library.domain.entity.Member;
import com.carlos.library.domain.entity.UserAccount;
import com.carlos.library.dto.ApiDtos.ProfileResponse;
import com.carlos.library.exception.NotFoundException;
import com.carlos.library.repository.MemberRepository;
import com.carlos.library.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserAccountRepository users;
    private final MemberRepository members;

    @Transactional(readOnly = true)
    public ProfileResponse me(String email) {
        UserAccount user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
        Member member = members.findByUserEmailIgnoreCase(email).orElse(null);
        return new ProfileResponse(user.getId(), member == null ? null : member.getId(), user.getName(),
                user.getEmail(), user.getRole(), user.isActive(), member == null ? null : member.getStatus(),
                member == null ? null : member.getRegistrationNumber());
    }
}
