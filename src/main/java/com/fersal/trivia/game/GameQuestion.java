package com.fersal.trivia.game;

import com.fersal.trivia.question.Question;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "game_questions")
public class GameQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "question_order", nullable = false)
    private short questionOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shuffled_option_ids", columnDefinition = "jsonb", nullable = false)
    private List<Long> shuffledOptionIds;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "shown_at")
    private Instant shownAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public Long getId() {
        return id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public short getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(short questionOrder) {
        this.questionOrder = questionOrder;
    }

    public List<Long> getShuffledOptionIds() {
        return shuffledOptionIds;
    }

    public void setShuffledOptionIds(List<Long> shuffledOptionIds) {
        this.shuffledOptionIds = shuffledOptionIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getShownAt() {
        return shownAt;
    }

    public void setShownAt(Instant shownAt) {
        this.shownAt = shownAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}