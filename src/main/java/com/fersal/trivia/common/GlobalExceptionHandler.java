package com.fersal.trivia.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final OperationalErrorRepository errorRepository;

    public GlobalExceptionHandler(OperationalErrorRepository errorRepository) {
        this.errorRepository = errorRepository;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        logOperationalError(ex.getErrorCode(), ex.getStatus().value(), request.getRequestURI(), ex.getMessage());
        ApiErrorResponse body = new ApiErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getStatus().value(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(". "));
        ApiErrorResponse body = new ApiErrorResponse(
                "VALIDATION_ERROR",
                msg,
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        logOperationalError("FILE_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE.value(), request.getRequestURI(), "Imagen supera el límite permitido.");
        ApiErrorResponse body = new ApiErrorResponse(
                "FILE_TOO_LARGE",
                "El archivo supera el tamaño máximo permitido (2 MB).",
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        logOperationalError("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getRequestURI(), ex.getMessage());
        ApiErrorResponse body = new ApiErrorResponse(
                "INTERNAL_ERROR",
                "Ocurrió un error interno al procesar la solicitud.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private void logOperationalError(String code, int status, String endpoint, String message) {
        try {
            OperationalError err = new OperationalError();
            err.setErrorCode(code);
            err.setHttpStatus((short) status);
            err.setEndpoint(endpoint);
            err.setMessage(message != null ? message : "Error sin detalle");
            errorRepository.save(err);
        } catch (Exception ignored) {}
    }
}