package com.fersal.trivia.ranking.dto;

import java.time.Instant;
import java.util.UUID;

public record CompetitionRankingItemDto(
        UUID gameId,
        String participantName,
        String categoryName,
        String tabletCode,
        int score,
        short correctCount,
        int totalResponseTimeMs,
        Instant finishedAt,
        String status
) {}