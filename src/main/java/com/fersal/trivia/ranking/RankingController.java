package com.fersal.trivia.ranking;

import com.fersal.trivia.ranking.dto.CompetitionRankingItemDto;
import com.fersal.trivia.ranking.dto.GeneralRankingItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/competition")
    public ResponseEntity<Page<CompetitionRankingItemDto>> getCompetitionRanking(
            @RequestParam(required = false) String sessionCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(rankingService.getCompetitionRanking(sessionCode, PageRequest.of(page, size)));
    }

    @GetMapping("/general")
    public ResponseEntity<Page<GeneralRankingItemDto>> getGeneralRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(rankingService.getGeneralRanking(PageRequest.of(page, size)));
    }
}