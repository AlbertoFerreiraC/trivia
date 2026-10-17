package com.fersal.trivia.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartGameRequest(
        @NotBlank(message = "El nombre del participante es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String participantName,

        @NotNull(message = "Debe seleccionar una categoría")
        Long categoryId,

        @NotBlank(message = "El código de la tablet es obligatorio (ej. TABLET_1)")
        String tabletCode
) {}