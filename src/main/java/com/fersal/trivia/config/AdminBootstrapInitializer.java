package com.fersal.trivia.config;

import com.fersal.trivia.security.AdminUser;
import com.fersal.trivia.security.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${trivia.admin.default-username:admin}")
    private String defaultUsername;

    @Value("${trivia.admin.default-password:fersal2026}")
    private String defaultPassword;

    public AdminBootstrapInitializer(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        adminUserRepository.findByUsername(defaultUsername).ifPresentOrElse(
                existing -> {
                    // Si el hash de la migración inicial no coincide, lo sincroniza al arrancar
                    if (!passwordEncoder.matches(defaultPassword, existing.getPasswordHash())) {
                        existing.setPasswordHash(passwordEncoder.encode(defaultPassword));
                        adminUserRepository.save(existing);
                    }
                },
                () -> {
                    AdminUser admin = new AdminUser();
                    admin.setUsername(defaultUsername);
                    admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
                    admin.setFullName("Administrador Seguridad Vial");
                    admin.setRole("ROLE_ADMIN");
                    admin.setActive(true);
                    adminUserRepository.save(admin);
                }
        );
    }
}