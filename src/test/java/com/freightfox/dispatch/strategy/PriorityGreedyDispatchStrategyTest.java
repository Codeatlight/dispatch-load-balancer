package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.distance.DistanceCalculator;
import com.freightfox.dispatch.distance.HaversineDistanceCalculator;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Priority;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.strategy.DispatchResult.UnassignedOrder;
import com.freightfox.dispatch.strategy.DispatchResult.VehicleAssignment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.freightfox.dispatch.TestData.CP_LAT;
import static com.freightfox.dispatch.TestData.CP_LON;
import static com.freightfox.dispatch.TestData.GURGAON_LAT;
import static com.freightfox.dispatch.TestData.GURGAON_LON;
import static com.freightfox.dispatch.TestData.NOIDA_LAT;
import static com.freightfox.dispatch.TestData.NOIDA_LON;
import static com.freightfox.dispatch.TestData.order;
import static com.freightfox.dispatch.TestData.vehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PriorityGreedyDispatchStrategyTest {

    private final DistanceCalculator distance = new HaversineDistanceCalculator();
    private final PriorityGreedyDispatchStrategy strategy = new PriorityGreedyDispatchStrategy(distance);

    @Test
    void assignsOrderToNearestVehicle() {
        Vehicle inNoida = vehicle("VEH001", 100, NOIDA_LAT, NOIDA_LON);
        Vehicle inGurgaon = vehicle("VEH002", 100, GURGAON_LAT, GURGAON_LON);
        DeliveryOrder noidaOrder = order("ORD001", NOIDA_LAT + 0.01, NOIDA_LON, 10, Priority.HIGH);

        DispatchResult result = strategy.plan(List.of(noidaOrder), List.of(inGurgaon, inNoida));

        assertThat(ordersOf(result, "VEH001")).containsExactly("ORD001");
        assertThat(ordersOf(result, "VEH002")).isEmpty();
        assertThat(result.unassignedOrders()).isEmpty();
    }

    @Test
    void highPriorityOrdersClaimCapacityBeforeLowPriority() {
        Vehicle van = vehicle("VEH001", 10, CP_LAT, CP_LON);
        DeliveryOrder nearbyLow = order("ORD001", CP_LAT, CP_LON, 10, Priority.LOW);
        DeliveryOrder distantHigh = order("ORD002", NOIDA_LAT, NOIDA_LON, 10, Priority.HIGH);

        DispatchResult result = strategy.plan(List.of(nearbyLow, distantHigh), List.of(van));

        assertThat(ordersOf(result, "VEH001")).containsExactly("ORD002");
        assertThat(result.unassignedOrders())
                .singleElement()
                .satisfies(unassigned -> {
                    assertThat(unassigned.order().getOrderId()).isEqualTo("ORD001");
                    assertThat(unassigned.reason()).isEqualTo(UnassignedReason.INSUFFICIENT_REMAINING_CAPACITY);
                });
    }

    @Test
    void deliversInPrioritySequenceWithinAVehicle() {
        Vehicle van = vehicle("VEH001", 100, CP_LAT, CP_LON);
        List<DeliveryOrder> orders = List.of(
                order("ORD001", CP_LAT, CP_LON, 5, Priority.LOW),
                order("ORD002", CP_LAT, CP_LON, 5, Priority.MEDIUM),
                order("ORD003", CP_LAT, CP_LON, 5, Priority.HIGH));

        DispatchResult result = strategy.plan(orders, List.of(van));

        assertThat(ordersOf(result, "VEH001")).containsExactly("ORD003", "ORD002", "ORD001");
    }

    @Test
    void placesHeavierPackagesFirstWithinTheSamePriority() {
        Vehicle van = vehicle("VEH001", 30, CP_LAT, CP_LON);
        DeliveryOrder light = order("ORD001", CP_LAT, CP_LON, 10, Priority.MEDIUM);
        DeliveryOrder heavy = order("ORD002", CP_LAT, CP_LON, 25, Priority.MEDIUM);

        DispatchResult result = strategy.plan(List.of(light, heavy), List.of(van));

        assertThat(ordersOf(result, "VEH001")).containsExactly("ORD002");
        assertThat(result.unassignedOrders()).extracting(u -> u.order().getOrderId()).containsExactly("ORD001");
    }

    @Test
    void spillsOverToNextNearestVehicleWhenNearestIsFull() {
        Vehicle nearButSmall = vehicle("VEH001", 10, CP_LAT, CP_LON);
        Vehicle farButLarge = vehicle("VEH002", 100, GURGAON_LAT, GURGAON_LON);
        List<DeliveryOrder> orders = List.of(
                order("ORD001", CP_LAT, CP_LON, 10, Priority.HIGH),
                order("ORD002", CP_LAT, CP_LON, 10, Priority.HIGH));

        DispatchResult result = strategy.plan(orders, List.of(nearButSmall, farButLarge));

        assertThat(ordersOf(result, "VEH001")).hasSize(1);
        assertThat(ordersOf(result, "VEH002")).hasSize(1);
        assertThat(result.unassignedOrders()).isEmpty();
    }

    @Test
    void allowsFillingAVehicleToExactlyItsCapacity() {
        Vehicle van = vehicle("VEH001", 30, CP_LAT, CP_LON);
        List<DeliveryOrder> orders = List.of(
                order("ORD001", CP_LAT, CP_LON, 10, Priority.HIGH),
                order("ORD002", CP_LAT, CP_LON, 20, Priority.HIGH));

        DispatchResult result = strategy.plan(orders, List.of(van));

        assertThat(assignmentOf(result, "VEH001").totalLoad()).isEqualByComparingTo("30");
        assertThat(result.unassignedOrders()).isEmpty();
    }

    @Test
    void reportsPackageHeavierThanEveryVehicle() {
        List<Vehicle> fleet = List.of(vehicle("VEH001", 50, CP_LAT, CP_LON), vehicle("VEH002", 80, CP_LAT, CP_LON));
        DeliveryOrder oversized = order("ORD001", CP_LAT, CP_LON, 81, Priority.HIGH);

        DispatchResult result = strategy.plan(List.of(oversized), fleet);

        assertThat(result.unassignedOrders())
                .extracting(UnassignedOrder::reason)
                .containsExactly(UnassignedReason.EXCEEDS_MAX_VEHICLE_CAPACITY);
        assertThat(result.assignments()).allSatisfy(a -> assertThat(a.orders()).isEmpty());
    }

    @Test
    void reportsEveryOrderAsUnassignedWhenThereAreNoVehicles() {
        List<DeliveryOrder> orders = List.of(
                order("ORD001", CP_LAT, CP_LON, 10, Priority.HIGH),
                order("ORD002", CP_LAT, CP_LON, 10, Priority.LOW));

        DispatchResult result = strategy.plan(orders, List.of());

        assertThat(result.assignments()).isEmpty();
        assertThat(result.unassignedOrders())
                .extracting(UnassignedOrder::reason)
                .containsOnly(UnassignedReason.NO_VEHICLES)
                .hasSize(2);
    }

    @Test
    void returnsIdleVehiclesWhenThereAreNoOrders() {
        DispatchResult result = strategy.plan(List.of(),
                List.of(vehicle("VEH002", 50, CP_LAT, CP_LON), vehicle("VEH001", 50, CP_LAT, CP_LON)));

        assertThat(result.assignments())
                .extracting(a -> a.vehicle().getVehicleId())
                .containsExactly("VEH001", "VEH002");
        assertThat(result.assignments()).allSatisfy(a -> {
            assertThat(a.orders()).isEmpty();
            assertThat(a.totalLoad()).isEqualByComparingTo("0");
            assertThat(a.totalDistanceKm()).isZero();
        });
    }

    @Test
    void totalDistanceFollowsTheRouteThroughEveryStop() {
        Vehicle van = vehicle("VEH001", 100, CP_LAT, CP_LON);
        DeliveryOrder toNoida = order("ORD001", NOIDA_LAT, NOIDA_LON, 10, Priority.HIGH);
        DeliveryOrder toGurgaon = order("ORD002", GURGAON_LAT, GURGAON_LON, 10, Priority.MEDIUM);

        DispatchResult result = strategy.plan(List.of(toGurgaon, toNoida), List.of(van));

        double expected = distance.distanceKm(van.location(), toNoida.location())
                + distance.distanceKm(toNoida.location(), toGurgaon.location());
        assertThat(assignmentOf(result, "VEH001").totalDistanceKm()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void breaksDistanceTiesByVehicleId() {
        List<Vehicle> fleet = List.of(vehicle("VEH002", 50, CP_LAT, CP_LON), vehicle("VEH001", 50, CP_LAT, CP_LON));

        DispatchResult result = strategy.plan(List.of(order("ORD001", CP_LAT, CP_LON, 10, Priority.HIGH)), fleet);

        assertThat(ordersOf(result, "VEH001")).containsExactly("ORD001");
    }

    @Test
    void handlesFractionalWeightsWithoutRoundingErrors() {
        Vehicle van = new Vehicle("VEH001", new BigDecimal("0.3"), CP_LAT, CP_LON, "CP");
        List<DeliveryOrder> orders = List.of(
                new DeliveryOrder("ORD001", CP_LAT, CP_LON, "CP", new BigDecimal("0.1"), Priority.HIGH),
                new DeliveryOrder("ORD002", CP_LAT, CP_LON, "CP", new BigDecimal("0.2"), Priority.HIGH));

        DispatchResult result = strategy.plan(orders, List.of(van));

        assertThat(result.unassignedOrders()).isEmpty();
    }

    @Test
    void neverExceedsCapacityAndAccountsForEveryOrderOnRandomInput() {
        Random random = new Random(42);
        List<Vehicle> fleet = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            fleet.add(vehicle("VEH%03d".formatted(i), 50 + random.nextInt(150), randomLat(random), randomLon(random)));
        }
        List<DeliveryOrder> orders = new ArrayList<>();
        Priority[] priorities = Priority.values();
        for (int i = 0; i < 2_000; i++) {
            orders.add(order("ORD%04d".formatted(i), randomLat(random), randomLon(random), 1 + random.nextInt(60),
                    priorities[random.nextInt(priorities.length)]));
        }

        DispatchResult result = strategy.plan(orders, fleet);

        Set<String> seen = new HashSet<>();
        for (VehicleAssignment assignment : result.assignments()) {
            BigDecimal load = assignment.orders().stream()
                    .map(DeliveryOrder::getPackageWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(load).isEqualByComparingTo(assignment.totalLoad());
            assertThat(load).isLessThanOrEqualTo(assignment.vehicle().getCapacity());
            assignment.orders().forEach(o -> assertThat(seen.add(o.getOrderId())).isTrue());
        }
        result.unassignedOrders().forEach(u -> assertThat(seen.add(u.order().getOrderId())).isTrue());
        assertThat(seen).hasSize(orders.size());
    }

    private static double randomLat(Random random) {
        return 28.4 + random.nextDouble() * 0.4;
    }

    private static double randomLon(Random random) {
        return 76.9 + random.nextDouble() * 0.6;
    }

    private static VehicleAssignment assignmentOf(DispatchResult result, String vehicleId) {
        return result.assignments().stream()
                .filter(a -> a.vehicle().getVehicleId().equals(vehicleId))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> ordersOf(DispatchResult result, String vehicleId) {
        return assignmentOf(result, vehicleId).orders().stream().map(DeliveryOrder::getOrderId).toList();
    }
}
