package com.fersal.trivia.game;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameQuestionRepository extends JpaRepository<GameQuestion, Long> {

    @EntityGraph(attributePaths = {"question", "question.options"})
    List<GameQuestion> findByGameIdOrderByQuestionOrderAsc(UUID gameId);

    @EntityGraph(attributePaths = {"question", "question.options"})
    Optional<GameQuestion> findByIdAndGameId(Long id, UUID gameId);

    @Query("SELECT gq.question.id FROM GameQuestion gq WHERE gq.game.id = :gameId ORDER BY gq.questionOrder ASC")
    List<Long> findQuestionIdsByGameId(@Param("gameId") UUID gameId);
}