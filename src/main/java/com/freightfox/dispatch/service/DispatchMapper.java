package com.freightfox.dispatch.service;

import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.OrderRequest;
import com.freightfox.dispatch.dto.OrderResponse;
import com.freightfox.dispatch.dto.UnassignedOrderResponse;
import com.freightfox.dispatch.dto.VehiclePlanResponse;
import com.freightfox.dispatch.dto.VehicleRequest;
import com.freightfox.dispatch.dto.VehicleResponse;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.strategy.DispatchResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class DispatchMapper {

    public DeliveryOrder toEntity(OrderRequest request) {
        return new DeliveryOrder(request.orderId().trim(), request.latitude(), request.longitude(),
                request.address().trim(), request.packageWeight(), request.priority());
    }

    public void updateEntity(DeliveryOrder order, OrderRequest request) {
        order.update(request.latitude(), request.longitude(), request.address().trim(),
                request.packageWeight(), request.priority());
    }

    public Vehicle toEntity(VehicleRequest request) {
        return new Vehicle(request.vehicleId().trim(), request.capacity(), request.currentLatitude(),
                request.currentLongitude(), request.currentAddress().trim());
    }

    public void updateEntity(Vehicle vehicle, VehicleRequest request) {
        vehicle.update(request.capacity(), request.currentLatitude(), request.currentLongitude(),
                request.currentAddress().trim());
    }

    public OrderResponse toResponse(DeliveryOrder order) {
        return new OrderResponse(order.getOrderId(), order.getLatitude(), order.getLongitude(), order.getAddress(),
                normalize(order.getPackageWeight()), order.getPriority());
    }

    public VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getVehicleId(), normalize(vehicle.getCapacity()),
                vehicle.getCurrentLatitude(), vehicle.getCurrentLongitude(), vehicle.getCurrentAddress());
    }

    public DispatchPlanResponse toResponse(DispatchResult result) {
        return new DispatchPlanResponse(
                result.assignments().stream()
                        .map(assignment -> new VehiclePlanResponse(
                                assignment.vehicle().getVehicleId(),
                                normalize(assignment.totalLoad()),
                                formatKm(assignment.totalDistanceKm()),
                                assignment.orders().stream().map(this::toResponse).toList()))
                        .toList(),
                result.unassignedOrders().stream()
                        .map(unassigned -> new UnassignedOrderResponse(
                                toResponse(unassigned.order()), unassigned.reason().getDescription()))
                        .toList());
    }

    static String formatKm(double km) {
        return String.format(Locale.ROOT, "%.2f km", km);
    }

    static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
