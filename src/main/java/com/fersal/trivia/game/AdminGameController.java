package com.fersal.trivia.game;

import com.fersal.trivia.common.OperationalError;
import com.fersal.trivia.common.OperationalErrorRepository;
import com.fersal.trivia.game.dto.GameSummaryResponse;
import com.fersal.trivia.participant.ParticipantRepository;
import com.fersal.trivia.question.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminGameController {

    private final GameRepository gameRepository;
    private final GameSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final ParticipantRepository participantRepository;
    private final OperationalErrorRepository errorRepository;
    private final JdbcTemplate jdbcTemplate;

    public AdminGameController(GameRepository gameRepository,
                               GameSessionRepository sessionRepository,
                               QuestionRepository questionRepository,
                               ParticipantRepository participantRepository,
                               OperationalErrorRepository errorRepository,
                               JdbcTemplate jdbcTemplate) {
        this.gameRepository = gameRepository;
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.participantRepository = participantRepository;
        this.errorRepository = errorRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        long totalQuestions = questionRepository.count();
        long totalParticipants = participantRepository.count();
        long totalGames = gameRepository.count();
        Integer activeLocks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tablet_question_locks", Integer.class);
        String activeSessionCode = sessionRepository.findFirstByStatusOrderByCreatedAtDesc("OPEN")
                .map(GameSession::getCode)
                .orElse("SIN_SESION");

        return ResponseEntity.ok(Map.of(
                "totalQuestions", totalQuestions,
                "totalParticipants", totalParticipants,
                "totalGames", totalGames,
                "lockedQuestionsCount", activeLocks != null ? activeLocks : 0,
                "activeSessionCode", activeSessionCode
        ));
    }

    @GetMapping("/games")
    public ResponseEntity<Page<GameSummaryResponse>> listGamesHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(gameRepository.findAllHistory(PageRequest.of(page, size)));
    }

    @PostMapping("/sessions/new")
    @Transactional
    public ResponseEntity<GameSession> startNewCompetitionSession(@RequestBody(required = false) Map<String, String> body) {
        sessionRepository.findFirstByStatusOrderByCreatedAtDesc("OPEN").ifPresent(current -> {
            current.setStatus("CLOSED");
            current.setClosedAt(Instant.now());
            sessionRepository.save(current);
        });

        // Al iniciar una nueva competencia general también liberamos los bloqueos retenidos de las tablets
        jdbcTemplate.update("DELETE FROM tablet_question_locks");

        GameSession next = new GameSession();
        next.setCode("COMP-" + Instant.now().toEpochMilli());
        String customName = (body != null && body.get("name") != null && !body.get("name").isBlank())
                ? body.get("name").trim()
                : "Competencia Seguridad Vial";
        next.setName(customName);
        next.setStatus("OPEN");
        return ResponseEntity.ok(sessionRepository.save(next));
    }

    @GetMapping("/errors")
    public ResponseEntity<Page<OperationalError>> listOperationalErrors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(errorRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)));
    }
}