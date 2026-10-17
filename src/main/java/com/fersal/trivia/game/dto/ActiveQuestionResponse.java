package com.fersal.trivia.game.dto;

import java.util.List;
import java.util.UUID;

public record ActiveQuestionResponse(
        UUID gameId,
        Long gameQuestionId,
        short questionOrder,
        int totalQuestions,
        String statement,
        String icon,
        String imageUrl,
        List<QuestionOptionDto> options,
        long remainingTimeMs,
        int currentTotalScore
) {
}