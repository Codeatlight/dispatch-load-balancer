package com.freightfox.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.math.BigDecimal;

@JsonPropertyOrder({"vehicleId", "capacity", "currentLatitude", "currentLongitude", "currentAddress"})
public record VehicleResponse(
        String vehicleId,
        BigDecimal capacity,
        double currentLatitude,
        double currentLongitude,
        String currentAddress) {
}
