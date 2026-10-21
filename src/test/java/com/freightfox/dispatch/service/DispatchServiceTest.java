package com.freightfox.dispatch.service;

import com.freightfox.dispatch.dto.OrderRequest;
import com.freightfox.dispatch.dto.VehicleRequest;
import com.freightfox.dispatch.exception.InvalidRequestException;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Priority;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.repository.DeliveryOrderRepository;
import com.freightfox.dispatch.repository.VehicleRepository;
import com.freightfox.dispatch.strategy.DispatchResult;
import com.freightfox.dispatch.strategy.DispatchStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static com.freightfox.dispatch.TestData.order;
import static com.freightfox.dispatch.TestData.vehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private DeliveryOrderRepository orderRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DispatchStrategy dispatchStrategy;

    private DispatchService service;

    @BeforeEach
    void setUp() {
        service = new DispatchService(orderRepository, vehicleRepository, dispatchStrategy, new DispatchMapper());
    }

    @Test
    @SuppressWarnings("unchecked")
    void upsertUpdatesExistingOrdersAndInsertsOnlyNewOnes() {
        DeliveryOrder existing = order("ORD001", 1, 1, 5, Priority.LOW);
        when(orderRepository.findAllById(List.of("ORD001", "ORD002"))).thenReturn(List.of(existing));

        service.saveOrders(List.of(
                orderRequest("ORD001", "50", Priority.HIGH),
                orderRequest("ORD002", "10", Priority.MEDIUM)));

        assertThat(existing.getPackageWeight()).isEqualByComparingTo("50");
        assertThat(existing.getPriority()).isEqualTo(Priority.HIGH);
        ArgumentCaptor<List<DeliveryOrder>> inserted = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).saveAll(inserted.capture());
        assertThat(inserted.getValue()).extracting(DeliveryOrder::getOrderId).containsExactly("ORD002");
        assertThat(inserted.getValue().getFirst().isNew()).isTrue();
    }

    @Test
    void trimsIdentifiersBeforeMatching() {
        when(orderRepository.findAllById(List.of("ORD001"))).thenReturn(List.of());

        service.saveOrders(List.of(orderRequest("  ORD001 ", "10", Priority.HIGH)));

        verify(orderRepository).findAllById(List.of("ORD001"));
    }

    @Test
    void rejectsDuplicateOrderIdsWithoutTouchingTheDatabase() {
        List<OrderRequest> requests = List.of(
                orderRequest("ORD001", "10", Priority.HIGH),
                orderRequest("ORD001", "20", Priority.LOW));

        assertThatThrownBy(() -> service.saveOrders(requests))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Duplicate orderId")
                .satisfies(ex -> assertThat(((InvalidRequestException) ex).getErrors())
                        .containsExactly("orderId 'ORD001' appears more than once"));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void rejectsDuplicateVehicleIds() {
        VehicleRequest request = new VehicleRequest("VEH001", BigDecimal.TEN, 1.0, 1.0, "Depot");

        assertThatThrownBy(() -> service.saveVehicles(List.of(request, request)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Duplicate vehicleId");
        verify(vehicleRepository, never()).saveAll(anyList());
    }

    @Test
    void upsertUpdatesExistingVehicle() {
        Vehicle existing = vehicle("VEH001", 100, 1, 1);
        when(vehicleRepository.findAllById(List.of("VEH001"))).thenReturn(List.of(existing));

        service.saveVehicles(List.of(new VehicleRequest("VEH001", new BigDecimal("150"), 2.0, 3.0, "New depot")));

        assertThat(existing.getCapacity()).isEqualByComparingTo("150");
        assertThat(existing.getCurrentAddress()).isEqualTo("New depot");
        verify(vehicleRepository).saveAll(List.of());
    }

    @Test
    void planDelegatesToStrategyWithAllStoredData() {
        List<DeliveryOrder> orders = List.of(order("ORD001", 1, 1, 5, Priority.HIGH));
        List<Vehicle> vehicles = List.of(vehicle("VEH001", 10, 1, 1));
        when(orderRepository.findAll()).thenReturn(orders);
        when(vehicleRepository.findAll()).thenReturn(vehicles);
        when(dispatchStrategy.plan(any(), any())).thenReturn(new DispatchResult(
                List.of(new DispatchResult.VehicleAssignment(vehicles.getFirst(), orders, new BigDecimal("5"), 0.0)),
                List.of()));

        var plan = service.getDispatchPlan();

        verify(dispatchStrategy).plan(orders, vehicles);
        assertThat(plan.dispatchPlan()).singleElement().satisfies(v -> {
            assertThat(v.vehicleId()).isEqualTo("VEH001");
            assertThat(v.totalDistance()).isEqualTo("0.00 km");
            assertThat(v.assignedOrders()).hasSize(1);
        });
    }

    private static OrderRequest orderRequest(String id, String weight, Priority priority) {
        return new OrderRequest(id, 28.6, 77.2, "Somewhere", new BigDecimal(weight), priority);
    }
}
