package com.freightfox.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"order", "reason"})
public record UnassignedOrderResponse(OrderResponse order, String reason) {
}
