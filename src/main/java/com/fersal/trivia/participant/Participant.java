package com.fersal.trivia.participant;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "participants")
public class Participant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 100)
    private String normalizedName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_played_at", nullable = false)
    private Instant lastPlayedAt = Instant.now();

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastPlayedAt() { return lastPlayedAt; }
    public void setLastPlayedAt(Instant lastPlayedAt) { this.lastPlayedAt = lastPlayedAt; }
}