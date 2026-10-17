package com.fersal.trivia.common;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "operational_errors")
public class OperationalError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "error_code", nullable = false, length = 60)
    private String errorCode;

    @Column(name = "http_status", nullable = false)
    private short httpStatus;

    @Column(nullable = false)
    private String endpoint;

    @Column(name = "tablet_code", length = 40)
    private String tabletCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public short getHttpStatus() { return httpStatus; }
    public void setHttpStatus(short httpStatus) { this.httpStatus = httpStatus; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getTabletCode() { return tabletCode; }
    public void setTabletCode(String tabletCode) { this.tabletCode = tabletCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getCreatedAt() { return createdAt; }
}