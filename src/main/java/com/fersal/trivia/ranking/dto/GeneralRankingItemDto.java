package com.fersal.trivia.ranking.dto;

public record GeneralRankingItemDto(
        Long participantId,
        String participantName,
        Long accumulatedScore,
        Long finishedGamesCount,
        Long totalCorrectAnswers,
        Integer bestTimeMs
) {
}