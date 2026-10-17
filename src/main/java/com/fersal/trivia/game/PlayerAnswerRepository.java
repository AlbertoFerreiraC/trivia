package com.fersal.trivia.game;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerAnswerRepository extends JpaRepository<PlayerAnswer, Long> {
}