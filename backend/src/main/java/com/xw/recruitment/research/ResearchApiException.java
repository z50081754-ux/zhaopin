package com.xw.recruitment.research;

import org.springframework.http.HttpStatus;

public class ResearchApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String safeMessage;

    public ResearchApiException(HttpStatus status, String code, String safeMessage) {
        super(safeMessage);
        this.status = status;
        this.code = code;
        this.safeMessage = safeMessage;
    }

    public static ResearchApiException badRequest(String code, String safeMessage) {
        return new ResearchApiException(HttpStatus.BAD_REQUEST, code, safeMessage);
    }

    public static ResearchApiException conflict(String code, String safeMessage) {
        return new ResearchApiException(HttpStatus.CONFLICT, code, safeMessage);
    }

    public static ResearchApiException unavailable(String code, String safeMessage) {
        return new ResearchApiException(HttpStatus.SERVICE_UNAVAILABLE, code, safeMessage);
    }

    public static ResearchApiException tooManyRequests(String code) {
        return new ResearchApiException(HttpStatus.TOO_MANY_REQUESTS, code,
            "Too many research submission attempts");
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String safeMessage() { return safeMessage; }
}
