package com.carlos.library.config;

import com.carlos.library.domain.entity.UserAccount;
import com.carlos.library.domain.enums.Role;
import com.carlos.library.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class BootstrapData implements ApplicationRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap.admin-name:Administrador}") private String name;
    @Value("${app.bootstrap.admin-email:admin@library.local}") private String email;
    @Value("${app.bootstrap.admin-password:Admin@123456}") private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase(email)) return;
        UserAccount admin = new UserAccount();
        admin.setName(name); admin.setEmail(email.toLowerCase(Locale.ROOT));
        admin.setPassword(encoder.encode(password)); admin.setRole(Role.ADMIN); admin.setActive(true);
        users.save(admin);
    }
}
