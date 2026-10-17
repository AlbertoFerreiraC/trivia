package com.fersal.trivia.game.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitAnswerRequest(
        @NotNull(message = "El identificador de la pregunta es obligatorio")
        Long gameQuestionId,
        Long selectedOptionId
) {
}