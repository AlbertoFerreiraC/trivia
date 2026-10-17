package com.fersal.trivia.game;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class GameCleanupScheduler {

    private final GameRepository gameRepository;

    public GameCleanupScheduler(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Cada 60 segundos marca como CANCELLED las partidas inconclusas que llevan más de 5 minutos sin actividad.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelStaleInProgressGames() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(5, ChronoUnit.MINUTES);
        gameRepository.cancelAbandonedGames(cutoff, now);
    }
}