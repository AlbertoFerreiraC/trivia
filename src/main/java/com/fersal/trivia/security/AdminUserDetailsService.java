package com.fersal.trivia.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    public AdminUserDetailsService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser admin = adminUserRepository.findByUsername(username)
                .filter(AdminUser::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("Administrador no encontrado o inactivo."));

        return new User(
                admin.getUsername(),
                admin.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(admin.getRole()))
        );
    }
}