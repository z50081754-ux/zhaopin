package com.xw.recruitment.research;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PublicResearchController.class)
public class PublicResearchExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> unreadableBody() {
        return validationFailure(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map<String, Object>> validationFailure(HttpStatus status) {
        return ResponseEntity.status(status).body(Map.of(
            "ok", false,
            "code", "VALIDATION_FAILED",
            "message", "Research submission validation failed"));
    }
}
