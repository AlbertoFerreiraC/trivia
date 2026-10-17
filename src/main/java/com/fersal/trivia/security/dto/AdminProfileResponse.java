package com.fersal.trivia.security.dto;

public record AdminProfileResponse(
        String username,
        String fullName,
        String role
) {
}