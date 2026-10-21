package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;

import java.math.BigDecimal;
import java.util.List;

public record DispatchResult(List<VehicleAssignment> assignments, List<UnassignedOrder> unassignedOrders) {

    public record VehicleAssignment(Vehicle vehicle, List<DeliveryOrder> orders, BigDecimal totalLoad,
                                    double totalDistanceKm) {
    }

    public record UnassignedOrder(DeliveryOrder order, UnassignedReason reason) {
    }
}
