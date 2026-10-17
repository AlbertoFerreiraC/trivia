package com.fersal.trivia.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveQuestionRequest(
        @NotNull(message = "La categoría es obligatoria")
        Long categoryId,

        @NotBlank(message = "El enunciado es obligatorio")
        String statement,

        @NotBlank(message = "La explicación educativa es obligatoria")
        String explanation,

        String icon,

        boolean active,

        @NotNull(message = "Debe enviar las 5 opciones de respuesta")
        @Size(min = 5, max = 5, message = "La pregunta debe tener exactamente 5 opciones")
        @Valid
        List<OptionInput> options
) {
    public record OptionInput(
            @NotBlank(message = "El texto de la opción no puede estar vacío")
            String text,
            boolean correct
    ) {
    }
}