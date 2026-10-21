package com.freightfox.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.freightfox.dispatch.model.Priority;

import java.math.BigDecimal;

@JsonPropertyOrder({"orderId", "latitude", "longitude", "address", "packageWeight", "priority"})
public record OrderResponse(
        String orderId,
        double latitude,
        double longitude,
        String address,
        BigDecimal packageWeight,
        Priority priority) {
}
