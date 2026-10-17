package com.fersal.trivia.security.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Ingrese el usuario") String username,
        @NotBlank(message = "Ingrese la contraseña") String password
) {
}