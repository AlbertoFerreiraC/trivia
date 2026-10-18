package com.fersal.trivia.game;

import org.springframework.stereotype.Component;

@Component
public class ScoringEngine {

    public static final int MAX_TIME_MS = 10000;

    public short calculatePoints(boolean isCorrect, long elapsedMs) {
        if (!isCorrect || elapsedMs < 0 || elapsedMs > MAX_TIME_MS) {
            return 0;
        }
        if (elapsedMs <= 2000) return 50;
        if (elapsedMs <= 4000) return 40;
        if (elapsedMs <= 6000) return 30;
        if (elapsedMs <= 8000) return 20;
        return 10;
    }
}