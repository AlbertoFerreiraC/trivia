package com.fersal.trivia.ranking;

import com.fersal.trivia.game.GameRepository;
import com.fersal.trivia.game.GameSession;
import com.fersal.trivia.game.GameSessionRepository;
import com.fersal.trivia.ranking.dto.CompetitionRankingItemDto;
import com.fersal.trivia.ranking.dto.GeneralRankingItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RankingService {

    private final GameRepository gameRepository;
    private final GameSessionRepository sessionRepository;

    public RankingService(GameRepository gameRepository, GameSessionRepository sessionRepository) {
        this.gameRepository = gameRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional(readOnly = true)
    public Page<CompetitionRankingItemDto> getCompetitionRanking(String sessionCode, Pageable pageable) {
        Long sessionId;
        if (sessionCode == null || sessionCode.isBlank()) {
            sessionId = sessionRepository.findFirstByStatusOrderByCreatedAtDesc("OPEN")
                    .map(GameSession::getId)
                    .orElse(1L);
        } else {
            sessionId = sessionRepository.findByCode(sessionCode)
                    .map(GameSession::getId)
                    .orElse(1L);
        }
        return gameRepository.findCompetitionRanking(sessionId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<GeneralRankingItemDto> getGeneralRanking(Pageable pageable) {
        return gameRepository.findGeneralRanking(pageable);
    }
}