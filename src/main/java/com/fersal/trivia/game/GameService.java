package com.fersal.trivia.game;

import com.fersal.trivia.category.Category;
import com.fersal.trivia.category.CategoryRepository;
import com.fersal.trivia.common.BusinessException;
import com.fersal.trivia.game.dto.*;
import com.fersal.trivia.participant.Participant;
import com.fersal.trivia.participant.ParticipantRepository;
import com.fersal.trivia.question.AnswerOption;
import com.fersal.trivia.question.Question;
import com.fersal.trivia.question.QuestionRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final GameSessionRepository sessionRepository;
    private final GameQuestionRepository gameQuestionRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final ParticipantRepository participantRepository;
    private final ScoringEngine scoringEngine;

    public GameService(GameRepository gameRepository,
                       GameSessionRepository sessionRepository,
                       GameQuestionRepository gameQuestionRepository,
                       PlayerAnswerRepository playerAnswerRepository,
                       CategoryRepository categoryRepository,
                       QuestionRepository questionRepository,
                       ParticipantRepository participantRepository,
                       ScoringEngine scoringEngine) {
        this.gameRepository = gameRepository;
        this.sessionRepository = sessionRepository;
        this.gameQuestionRepository = gameQuestionRepository;
        this.playerAnswerRepository = playerAnswerRepository;
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
        this.participantRepository = participantRepository;
        this.scoringEngine = scoringEngine;
    }

    @Transactional
    public StartGameResponse startNewGame(StartGameRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .filter(Category::isActive)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "La categoría seleccionada no existe o está inactiva."));

        GameSession activeSession = sessionRepository.findFirstByStatusOrderByCreatedAtDesc("OPEN")
                .orElseGet(() -> {
                    GameSession s = new GameSession();
                    s.setCode("COMP-" + Instant.now().toEpochMilli());
                    s.setName("Competencia General de Seguridad Vial");
                    s.setStatus("OPEN");
                    return sessionRepository.save(s);
                });

        String cleanName = request.participantName().trim();
        String normalized = cleanName.toLowerCase(Locale.ROOT);
        Participant participant = participantRepository.findByNormalizedName(normalized)
                .map(existing -> {
                    existing.setLastPlayedAt(Instant.now());
                    return participantRepository.save(existing);
                })
                .orElseGet(() -> {
                    Participant p = new Participant();
                    p.setDisplayName(cleanName);
                    p.setNormalizedName(normalized);
                    return participantRepository.save(p);
                });

        String tabletCode = request.tabletCode().trim().toUpperCase(Locale.ROOT);

        // Si esta tablet dejó una partida anterior en curso, la cancelamos antes de abrir la nueva
        gameRepository.findByTabletCodeAndStatus(tabletCode, "IN_PROGRESS")
                .forEach(unfinished -> {
                    unfinished.setStatus("CANCELLED");
                    unfinished.setFinishedAt(Instant.now());
                    gameRepository.save(unfinished);
                });

        Game game = new Game();
        game.setSession(activeSession);
        game.setParticipant(participant);
        game.setCategory(category);
        game.setTabletCode(tabletCode);
        game.setStatus("IN_PROGRESS");
        game = gameRepository.saveAndFlush(game);

        List<Long> lockedQuestionIds;
        try {
            lockedQuestionIds = questionRepository.assignAndLockQuestionsForTablet(
                    game.getId(), tabletCode, category.getId());
        } catch (DataAccessException ex) {
            throw new BusinessException("INSUFFICIENT_QUESTIONS",
                    "No hay 5 preguntas libres disponibles en esta categoría en este momento.", HttpStatus.CONFLICT);
        }

        if (lockedQuestionIds == null || lockedQuestionIds.size() < 5) {
            throw new BusinessException("INSUFFICIENT_QUESTIONS",
                    "No hay suficientes preguntas libres en esta categoría.", HttpStatus.CONFLICT);
        }

        Map<Long, Question> questionMap = questionRepository.findByIdIn(lockedQuestionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        short order = 1;
        for (Long qId : lockedQuestionIds) {
            Question q = questionMap.get(qId);
            List<Long> optionIds = q.getOptions().stream()
                    .map(AnswerOption::getId)
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(optionIds);

            GameQuestion gq = new GameQuestion();
            gq.setGame(game);
            gq.setQuestion(q);
            gq.setQuestionOrder(order++);
            gq.setShuffledOptionIds(optionIds);
            gq.setStatus("PENDING");
            gameQuestionRepository.save(gq);
        }

        return new StartGameResponse(
                game.getId(),
                activeSession.getCode(),
                participant.getDisplayName(),
                category.getName(),
                tabletCode,
                5
        );
    }

    @Transactional
    public ActiveQuestionResponse getNextOrCurrentQuestion(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException("GAME_NOT_FOUND", "Partida no encontrada.", HttpStatus.NOT_FOUND));

        if (!"IN_PROGRESS".equals(game.getStatus())) {
            throw new BusinessException("GAME_ALREADY_FINISHED", "La partida ya ha finalizado.");
        }

        List<GameQuestion> questions = gameQuestionRepository.findByGameIdOrderByQuestionOrderAsc(gameId);
        Instant now = Instant.now();

        Optional<GameQuestion> currentActive = questions.stream()
                .filter(q -> "ACTIVE".equals(q.getStatus()))
                .findFirst();

        if (currentActive.isPresent()) {
            GameQuestion activeQ = currentActive.get();
            if (now.isAfter(activeQ.getExpiresAt())) {
                closeExpiredQuestion(game, activeQ, now);
            } else {
                return toActiveQuestionResponse(game, activeQ, now);
            }
        }

        Optional<GameQuestion> nextPending = questions.stream()
                .filter(q -> "PENDING".equals(q.getStatus()))
                .findFirst();

        if (nextPending.isEmpty()) {
            finishGameIfCompleted(game);
            throw new BusinessException("NO_MORE_QUESTIONS", "Se han completado las 5 preguntas de la partida.");
        }

        GameQuestion nextQ = nextPending.get();
        nextQ.setStatus("ACTIVE");
        nextQ.setShownAt(now);
        nextQ.setExpiresAt(now.plusMillis(ScoringEngine.MAX_TIME_MS));
        gameQuestionRepository.save(nextQ);

        return toActiveQuestionResponse(game, nextQ, now);
    }

    @Transactional
    public AnswerEvaluationResponse submitAnswer(UUID gameId, SubmitAnswerRequest request) {
        Instant serverReceivedAt = Instant.now();

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException("GAME_NOT_FOUND", "Partida no encontrada.", HttpStatus.NOT_FOUND));

        if (!"IN_PROGRESS".equals(game.getStatus())) {
            throw new BusinessException("GAME_ALREADY_FINISHED", "No se pueden enviar respuestas a una partida finalizada.");
        }

        GameQuestion gq = gameQuestionRepository.findByIdAndGameId(request.gameQuestionId(), gameId)
                .orElseThrow(() -> new BusinessException("QUESTION_NOT_IN_GAME", "La pregunta no pertenece a esta partida."));

        if ("ANSWERED".equals(gq.getStatus()) || "EXPIRED".equals(gq.getStatus())) {
            throw new BusinessException("QUESTION_ALREADY_ANSWERED", "Esta pregunta ya fue respondida o su tiempo expiró.");
        }

        if (gq.getShownAt() == null) {
            throw new BusinessException("QUESTION_NOT_SHOWN", "La pregunta aún no fue activada por el servidor.");
        }

        long rawElapsedMs = Duration.between(gq.getShownAt(), serverReceivedAt).toMillis();
        int elapsedMs = (int) Math.max(0, rawElapsedMs);
        boolean timedOut = elapsedMs > ScoringEngine.MAX_TIME_MS || request.selectedOptionId() == null;

        Question question = gq.getQuestion();
        AnswerOption correctOption = question.getOptions().stream()
                .filter(AnswerOption::isCorrect)
                .findFirst()
                .orElseThrow();

        AnswerOption chosenOption = null;
        if (request.selectedOptionId() != null) {
            chosenOption = question.getOptions().stream()
                    .filter(o -> o.getId().equals(request.selectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("INVALID_OPTION", "La opción seleccionada no pertenece a esta pregunta."));
        }

        boolean isCorrect = !timedOut && chosenOption != null && chosenOption.isCorrect();
        short awardedPoints = scoringEngine.calculatePoints(isCorrect, elapsedMs);

        PlayerAnswer answer = new PlayerAnswer();
        answer.setGameQuestion(gq);
        answer.setSelectedOption(chosenOption);
        answer.setServerReceivedAt(serverReceivedAt);
        answer.setElapsedMs(Math.min(elapsedMs, ScoringEngine.MAX_TIME_MS));
        answer.setCorrect(isCorrect);
        answer.setAwardedPoints(awardedPoints);
        playerAnswerRepository.save(answer);

        gq.setStatus(timedOut ? "EXPIRED" : "ANSWERED");
        gameQuestionRepository.save(gq);

        game.setTotalScore(game.getTotalScore() + awardedPoints);
        if (isCorrect) {
            game.setCorrectCount((short) (game.getCorrectCount() + 1));
        }
        game.setTotalResponseTimeMs(game.getTotalResponseTimeMs() + Math.min(elapsedMs, ScoringEngine.MAX_TIME_MS));

        boolean isLastQuestion = gq.getQuestionOrder() >= 5;
        if (isLastQuestion) {
            game.setStatus("FINISHED");
            game.setFinishedAt(serverReceivedAt);
        }
        gameRepository.save(game);

        return new AnswerEvaluationResponse(
                gq.getId(),
                gq.getQuestionOrder(),
                isCorrect,
                timedOut,
                awardedPoints,
                elapsedMs,
                correctOption.getId(),
                correctOption.getOptionText(),
                question.getExplanation(),
                game.getTotalScore(),
                game.getCorrectCount(),
                "FINISHED".equals(game.getStatus())
        );
    }

    @Transactional(readOnly = true)
    public GameSummaryResponse getGameSummary(UUID gameId) {
        Game game = gameRepository.findByIdWithDetails(gameId)
                .orElseThrow(() -> new BusinessException("GAME_NOT_FOUND", "Partida no encontrada.", HttpStatus.NOT_FOUND));

        return new GameSummaryResponse(
                game.getId(),
                game.getSession().getCode(),
                game.getParticipant().getDisplayName(),
                game.getCategory().getName(),
                game.getTabletCode(),
                game.getStatus(),
                game.getTotalScore(),
                game.getCorrectCount(),
                5,
                game.getTotalResponseTimeMs(),
                game.getStartedAt(),
                game.getFinishedAt()
        );
    }

    private void closeExpiredQuestion(Game game, GameQuestion activeQ, Instant now) {
        PlayerAnswer timeoutAnswer = new PlayerAnswer();
        timeoutAnswer.setGameQuestion(activeQ);
        timeoutAnswer.setSelectedOption(null);
        timeoutAnswer.setServerReceivedAt(now);
        timeoutAnswer.setElapsedMs(ScoringEngine.MAX_TIME_MS);
        timeoutAnswer.setCorrect(false);
        timeoutAnswer.setAwardedPoints((short) 0);
        playerAnswerRepository.save(timeoutAnswer);

        activeQ.setStatus("EXPIRED");
        gameQuestionRepository.save(activeQ);

        game.setTotalResponseTimeMs(game.getTotalResponseTimeMs() + ScoringEngine.MAX_TIME_MS);
        if (activeQ.getQuestionOrder() >= 5) {
            game.setStatus("FINISHED");
            game.setFinishedAt(now);
        }
        gameRepository.save(game);
    }

    private void finishGameIfCompleted(Game game) {
        if ("IN_PROGRESS".equals(game.getStatus())) {
            game.setStatus("FINISHED");
            game.setFinishedAt(Instant.now());
            gameRepository.save(game);
        }
    }

    private ActiveQuestionResponse toActiveQuestionResponse(Game game, GameQuestion gq, Instant now) {
        Question q = gq.getQuestion();
        Map<Long, AnswerOption> optionById = q.getOptions().stream()
                .collect(Collectors.toMap(AnswerOption::getId, Function.identity()));

        List<QuestionOptionDto> shuffledOptions = gq.getShuffledOptionIds().stream()
                .map(optId -> {
                    AnswerOption opt = optionById.get(optId);
                    return new QuestionOptionDto(opt.getId(), opt.getOptionText());
                })
                .toList();

        long remainingMs = Math.max(0, Duration.between(now, gq.getExpiresAt()).toMillis());
        String imageUrl = q.getImageFilename() != null ? "/uploads/questions/" + q.getImageFilename() : null;

        return new ActiveQuestionResponse(
                game.getId(),
                gq.getId(),
                gq.getQuestionOrder(),
                5,
                q.getStatement(),
                q.getIcon(),
                imageUrl,
                shuffledOptions,
                remainingMs,
                game.getTotalScore()
        );
    }
}