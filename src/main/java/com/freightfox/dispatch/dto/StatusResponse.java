package com.freightfox.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"message", "status"})
public record StatusResponse(String message, String status) {

    public static StatusResponse success(String message) {
        return new StatusResponse(message, "success");
    }
}
