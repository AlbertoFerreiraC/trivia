package com.fersal.trivia.common;

import java.time.Instant;

public record ApiErrorResponse(
        String errorCode,
        String message,
        int status,
        String path,
        Instant timestamp
) {
}