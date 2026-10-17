package com.fersal.trivia.security;

import com.fersal.trivia.common.BusinessException;
import com.fersal.trivia.security.dto.AdminProfileResponse;
import com.fersal.trivia.security.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AdminUserRepository adminUserRepository;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          AdminUserRepository adminUserRepository,
                          SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.adminUserRepository = adminUserRepository;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AdminProfileResponse> login(@Valid @RequestBody LoginRequest request,
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username().trim(), request.password())
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            AdminUser admin = adminUserRepository.findByUsername(request.username().trim()).orElseThrow();
            return ResponseEntity.ok(new AdminProfileResponse(admin.getUsername(), admin.getFullName(), admin.getRole()));
        } catch (BadCredentialsException ex) {
            throw new BusinessException("INVALID_CREDENTIALS", "Usuario o contraseña incorrectos.", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente."));
    }

    @GetMapping("/me")
    public ResponseEntity<AdminProfileResponse> currentAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("UNAUTHORIZED", "Sesión no iniciada.", HttpStatus.UNAUTHORIZED);
        }
        AdminUser admin = adminUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "Usuario no encontrado.", HttpStatus.UNAUTHORIZED));
        return ResponseEntity.ok(new AdminProfileResponse(admin.getUsername(), admin.getFullName(), admin.getRole()));
    }
}