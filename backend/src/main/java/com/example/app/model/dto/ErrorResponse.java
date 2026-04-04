package com.example.app.model.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(String code, String message, List<ErrorDetail> details, String timestamp) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), Instant.now().toString());
    }

    public static ErrorResponse of(String code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(code, message, details, Instant.now().toString());
    }

    public record ErrorDetail(String field, String message) {
    }
}
