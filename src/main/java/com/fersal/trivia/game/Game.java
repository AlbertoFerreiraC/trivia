package com.fersal.trivia.game;

import com.fersal.trivia.category.Category;
import com.fersal.trivia.participant.Participant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "tablet_code", nullable = false, length = 40)
    private String tabletCode;

    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    @Column(name = "total_score", nullable = false)
    private int totalScore = 0;

    @Column(name = "correct_count", nullable = false)
    private short correctCount = 0;

    @Column(name = "total_response_time_ms", nullable = false)
    private int totalResponseTimeMs = 0;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Version
    private Long version;

    public UUID getId() {
        return id;
    }

    public GameSession getSession() {
        return session;
    }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getTabletCode() {
        return tabletCode;
    }

    public void setTabletCode(String tabletCode) {
        this.tabletCode = tabletCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public short getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(short correctCount) {
        this.correctCount = correctCount;
    }

    public int getTotalResponseTimeMs() {
        return totalResponseTimeMs;
    }

    public void setTotalResponseTimeMs(int totalResponseTimeMs) {
        this.totalResponseTimeMs = totalResponseTimeMs;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}