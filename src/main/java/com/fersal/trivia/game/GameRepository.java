package com.fersal.trivia.game;

import com.fersal.trivia.game.dto.GameSummaryResponse;
import com.fersal.trivia.ranking.dto.CompetitionRankingItemDto;
import com.fersal.trivia.ranking.dto.GeneralRankingItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    List<Game> findByTabletCodeAndStatus(String tabletCode, String status);

    @Query("""
                SELECT g FROM Game g
                JOIN FETCH g.session
                JOIN FETCH g.participant
                JOIN FETCH g.category
                WHERE g.id = :id
            """)
    Optional<Game> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
                SELECT new com.fersal.trivia.game.dto.GameSummaryResponse(
                    g.id, s.code, p.displayName, c.name, g.tabletCode, g.status,
                    g.totalScore, g.correctCount, 5, g.totalResponseTimeMs, g.startedAt, g.finishedAt
                )
                FROM Game g
                JOIN g.session s
                JOIN g.participant p
                JOIN g.category c
                ORDER BY g.startedAt DESC
            """)
    Page<GameSummaryResponse> findAllHistory(Pageable pageable);

    @Query("""
                SELECT new com.fersal.trivia.ranking.dto.CompetitionRankingItemDto(
                    g.id, p.displayName, c.name, g.tabletCode, g.totalScore,
                    g.correctCount, g.totalResponseTimeMs, g.finishedAt, g.status
                )
                FROM Game g
                JOIN g.participant p
                JOIN g.category c
                WHERE g.session.id = :sessionId
                  AND g.status = 'FINISHED'
                ORDER BY g.totalScore DESC, g.totalResponseTimeMs ASC, g.finishedAt ASC, g.id ASC
            """)
    Page<CompetitionRankingItemDto> findCompetitionRanking(@Param("sessionId") Long sessionId, Pageable pageable);

    @Query("""
                SELECT new com.fersal.trivia.ranking.dto.GeneralRankingItemDto(
                    p.id, p.displayName, SUM(g.totalScore), COUNT(g.id),
                    SUM(g.correctCount), MIN(g.totalResponseTimeMs)
                )
                FROM Game g
                JOIN g.participant p
                WHERE g.status = 'FINISHED'
                GROUP BY p.id, p.displayName
                ORDER BY SUM(g.totalScore) DESC, COUNT(g.id) ASC, MIN(g.totalResponseTimeMs) ASC, p.id ASC
            """)
    Page<GeneralRankingItemDto> findGeneralRanking(Pageable pageable);

    @Modifying
    @Query("UPDATE Game g SET g.status = 'CANCELLED', g.finishedAt = :now WHERE g.status = 'IN_PROGRESS' AND g.startedAt < :cutoff")
    int cancelAbandonedGames(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}