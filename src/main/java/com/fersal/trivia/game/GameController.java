package com.fersal.trivia.game;

import com.fersal.trivia.game.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<StartGameResponse> startGame(@Valid @RequestBody StartGameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.startNewGame(request));
    }

    @GetMapping("/{gameId}/current-question")
    public ResponseEntity<ActiveQuestionResponse> getCurrentQuestion(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getNextOrCurrentQuestion(gameId));
    }

    @PostMapping("/{gameId}/answers")
    public ResponseEntity<AnswerEvaluationResponse> submitAnswer(
            @PathVariable UUID gameId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        return ResponseEntity.ok(gameService.submitAnswer(gameId, request));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameSummaryResponse> getGameSummary(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getGameSummary(gameId));
    }
}