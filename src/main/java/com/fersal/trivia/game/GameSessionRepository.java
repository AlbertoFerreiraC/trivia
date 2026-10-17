package com.fersal.trivia.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    Optional<GameSession> findFirstByStatusOrderByCreatedAtDesc(String status);

    Optional<GameSession> findByCode(String code);
}