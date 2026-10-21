package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.distance.DistanceCalculator;
import com.freightfox.dispatch.distance.GeoPoint;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.strategy.DispatchResult.UnassignedOrder;
import com.freightfox.dispatch.strategy.DispatchResult.VehicleAssignment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class PriorityGreedyDispatchStrategy implements DispatchStrategy {

    static final Comparator<DeliveryOrder> DISPATCH_SEQUENCE = Comparator
            .comparing(DeliveryOrder::getPriority)
            .thenComparing(DeliveryOrder::getPackageWeight, Comparator.reverseOrder())
            .thenComparing(DeliveryOrder::getOrderId);

    private final DistanceCalculator distanceCalculator;

    public PriorityGreedyDispatchStrategy(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public DispatchResult plan(List<DeliveryOrder> orders, List<Vehicle> vehicles) {
        List<Route> routes = vehicles.stream()
                .sorted(Comparator.comparing(Vehicle::getVehicleId))
                .map(Route::new)
                .toList();
        BigDecimal largestCapacity = vehicles.stream()
                .map(Vehicle::getCapacity)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        List<UnassignedOrder> unassigned = new ArrayList<>();
        for (DeliveryOrder order : orders.stream().sorted(DISPATCH_SEQUENCE).toList()) {
            Route nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Route route : routes) {
                if (!route.canCarry(order)) {
                    continue;
                }
                double distance = distanceCalculator.distanceKm(route.position, order.location());
                if (distance < nearestDistance) {
                    nearest = route;
                    nearestDistance = distance;
                }
            }

            if (nearest != null) {
                nearest.assign(order, nearestDistance);
            } else {
                unassigned.add(new UnassignedOrder(order, reasonFor(order, routes.isEmpty(), largestCapacity)));
            }
        }

        return new DispatchResult(routes.stream().map(Route::toAssignment).toList(), unassigned);
    }

    private static UnassignedReason reasonFor(DeliveryOrder order, boolean noVehicles, BigDecimal largestCapacity) {
        if (noVehicles) {
            return UnassignedReason.NO_VEHICLES;
        }
        if (order.getPackageWeight().compareTo(largestCapacity) > 0) {
            return UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY;
        }
        return UnassignedReason.INSUFFICIENT_REMAINING_CAPACITY;
    }

    private static final class Route {
        private final Vehicle vehicle;
        private final List<DeliveryOrder> orders = new ArrayList<>();
        private BigDecimal load = BigDecimal.ZERO;
        private double distanceKm;
        private GeoPoint position;

        private Route(Vehicle vehicle) {
            this.vehicle = vehicle;
            this.position = vehicle.location();
        }

        private boolean canCarry(DeliveryOrder order) {
            return load.add(order.getPackageWeight()).compareTo(vehicle.getCapacity()) <= 0;
        }

        private void assign(DeliveryOrder order, double legDistanceKm) {
            orders.add(order);
            load = load.add(order.getPackageWeight());
            distanceKm += legDistanceKm;
            position = order.location();
        }

        private VehicleAssignment toAssignment() {
            return new VehicleAssignment(vehicle, List.copyOf(orders), load, distanceKm);
        }
    }
}
