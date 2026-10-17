package com.fersal.trivia.game;

import com.fersal.trivia.question.AnswerOption;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "player_answers")
public class PlayerAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_question_id", nullable = false, unique = true)
    private GameQuestion gameQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private AnswerOption selectedOption;

    @Column(name = "server_received_at", nullable = false)
    private Instant serverReceivedAt = Instant.now();

    @Column(name = "elapsed_ms", nullable = false)
    private int elapsedMs;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "awarded_points", nullable = false)
    private short awardedPoints;

    public Long getId() {
        return id;
    }

    public GameQuestion getGameQuestion() {
        return gameQuestion;
    }

    public void setGameQuestion(GameQuestion gameQuestion) {
        this.gameQuestion = gameQuestion;
    }

    public AnswerOption getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(AnswerOption selectedOption) {
        this.selectedOption = selectedOption;
    }

    public Instant getServerReceivedAt() {
        return serverReceivedAt;
    }

    public void setServerReceivedAt(Instant serverReceivedAt) {
        this.serverReceivedAt = serverReceivedAt;
    }

    public int getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(int elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public short getAwardedPoints() {
        return awardedPoints;
    }

    public void setAwardedPoints(short awardedPoints) {
        this.awardedPoints = awardedPoints;
    }
}