package com.fersal.trivia.game.dto;

public record AnswerEvaluationResponse(
        Long gameQuestionId,
        short questionOrder,
        boolean correct,
        boolean timedOut,
        short awardedPoints,
        int serverElapsedMs,
        Long correctOptionId,
        String correctOptionText,
        String explanation,
        int updatedTotalScore,
        short updatedCorrectCount,
        boolean gameFinished
) {
}