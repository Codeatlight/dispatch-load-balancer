package com.freightfox.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.util.List;

@JsonPropertyOrder({"status", "code", "message", "errors", "timestamp"})
public record ErrorResponse(String status, int code, String message, List<String> errors, Instant timestamp) {

    public static ErrorResponse of(int code, String message, List<String> errors) {
        return new ErrorResponse("error", code, message, errors, Instant.now());
    }
}
