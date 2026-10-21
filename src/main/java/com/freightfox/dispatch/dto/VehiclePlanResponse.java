package com.freightfox.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@JsonPropertyOrder({"vehicleId", "totalLoad", "totalDistance", "assignedOrders"})
public record VehiclePlanResponse(
        String vehicleId,
        BigDecimal totalLoad,
        @Schema(example = "12.34 km", description = "Distance from the vehicle's current location through every assigned stop, in order")
        String totalDistance,
        List<OrderResponse> assignedOrders) {
}
