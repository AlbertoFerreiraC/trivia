package com.fersal.trivia.game.dto;

import java.util.UUID;

public record StartGameResponse(
        UUID gameId,
        String sessionCode,
        String participantName,
        String categoryName,
        String tabletCode,
        int totalQuestions
) {
}