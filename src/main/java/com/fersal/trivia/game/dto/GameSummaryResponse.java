package com.fersal.trivia.game.dto;

import java.time.Instant;
import java.util.UUID;

public record GameSummaryResponse(
        UUID gameId,
        String sessionCode,
        String participantName,
        String categoryName,
        String tabletCode,
        String status,
        int totalScore,
        short correctCount,
        int totalQuestions,
        int totalResponseTimeMs,
        Instant startedAt,
        Instant finishedAt
) {
}