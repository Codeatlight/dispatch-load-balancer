package com.freightfox.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"dispatchPlan", "unassignedOrders"})
public record DispatchPlanResponse(
        List<VehiclePlanResponse> dispatchPlan,
        List<UnassignedOrderResponse> unassignedOrders) {
}
