package com.xw.recruitment.config;

import com.xw.recruitment.research.ResearchApiException;
import com.xw.recruitment.research.PublicResearchController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResearchApiException.class)
    ResponseEntity<Map<String, Object>> researchError(ResearchApiException exception) {
        return ResponseEntity.status(exception.status()).body(Map.of(
            "ok", false,
            "code", exception.code(),
            "message", exception.safeMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> validationFailed(MethodArgumentNotValidException exception) {
        if (exception.getParameter().getContainingClass() == PublicResearchController.class) {
            return Map.of("ok", false, "code", "VALIDATION_FAILED",
                "message", "Request validation failed");
        }
        return Map.of("ok", false, "code", "INVALID_REQUEST",
            "message", exception.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Map<String, Object>> unsupportedMediaType(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request)
            throws HttpMediaTypeNotSupportedException {
        if (!"POST".equals(request.getMethod())
                || !"/api/research/submissions".equals(request.getRequestURI())) {
            throw exception;
        }
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
            "ok", false,
            "code", "VALIDATION_FAILED",
            "message", "Research submission validation failed"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, Object> badRequest() {
        return Map.of("ok", false, "code", "INVALID_REQUEST", "message", "Invalid request");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    Map<String, Object> unauthorized() {
        return Map.of("ok", false, "code", "INVALID_CREDENTIALS");
    }
}
