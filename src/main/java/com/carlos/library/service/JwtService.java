package com.carlos.library.service;

import com.carlos.library.domain.entity.UserAccount;
import com.carlos.library.dto.ApiDtos.TokenResponse;
import com.carlos.library.exception.BusinessException;
import com.carlos.library.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final UserAccountRepository users;

    @Value("${app.security.jwt.access-minutes:30}")
    private long accessMinutes;
    @Value("${app.security.jwt.refresh-days:7}")
    private long refreshDays;
    @Value("${spring.application.name:library-management-api}")
    private String issuer;

    public TokenResponse issue(UserAccount user) {
        String access = encode(user, "access", Instant.now().plus(accessMinutes, ChronoUnit.MINUTES));
        String refresh = encode(user, "refresh", Instant.now().plus(refreshDays, ChronoUnit.DAYS));
        return new TokenResponse("Bearer", access, refresh, accessMinutes * 60);
    }

    public TokenResponse refresh(String refreshToken) {
        try {
            Jwt jwt = decoder.decode(refreshToken);
            if (!"refresh".equals(jwt.getClaimAsString("type"))) {
                throw new BusinessException("Token de atualização inválido.");
            }
            UserAccount user = users.findByEmailIgnoreCase(jwt.getSubject())
                    .filter(UserAccount::isActive)
                    .orElseThrow(() -> new BusinessException("Usuário inativo ou inexistente."));
            return issue(user);
        } catch (JwtException ex) {
            throw new BusinessException("Token de atualização inválido ou expirado.");
        }
    }

    private String encode(UserAccount user, String type, Instant expiresAt) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("name", user.getName())
                .claim("type", type);
        if ("access".equals(type)) {
            builder.claim("roles", List.of("ROLE_" + user.getRole().name()));
        }
        JwtClaimsSet claims = builder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
