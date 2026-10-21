package com.freightfox.dispatch.service;

import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.OrderListResponse;
import com.freightfox.dispatch.dto.OrderRequest;
import com.freightfox.dispatch.dto.VehicleListResponse;
import com.freightfox.dispatch.dto.VehicleRequest;
import com.freightfox.dispatch.exception.InvalidRequestException;
import com.freightfox.dispatch.model.AssignedIdEntity;
import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;
import com.freightfox.dispatch.repository.DeliveryOrderRepository;
import com.freightfox.dispatch.repository.VehicleRepository;
import com.freightfox.dispatch.strategy.DispatchStrategy;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class DispatchService {

    private final DeliveryOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DispatchStrategy dispatchStrategy;
    private final DispatchMapper mapper;

    public DispatchService(DeliveryOrderRepository orderRepository, VehicleRepository vehicleRepository,
                           DispatchStrategy dispatchStrategy, DispatchMapper mapper) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.dispatchStrategy = dispatchStrategy;
        this.mapper = mapper;
    }

    public void saveOrders(List<OrderRequest> requests) {
        upsert(requests, request -> request.orderId().trim(), "orderId", orderRepository,
                mapper::toEntity, mapper::updateEntity);
    }

    public void saveVehicles(List<VehicleRequest> requests) {
        upsert(requests, request -> request.vehicleId().trim(), "vehicleId", vehicleRepository,
                mapper::toEntity, mapper::updateEntity);
    }

    @Transactional(readOnly = true)
    public OrderListResponse getOrders() {
        return new OrderListResponse(orderRepository.findAll(Sort.by("orderId")).stream()
                .map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public VehicleListResponse getVehicles() {
        return new VehicleListResponse(vehicleRepository.findAll(Sort.by("vehicleId")).stream()
                .map(mapper::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public DispatchPlanResponse getDispatchPlan() {
        return mapper.toResponse(dispatchStrategy.plan(orderRepository.findAll(), vehicleRepository.findAll()));
    }

    public void deleteAllOrders() {
        orderRepository.deleteAllInBatch();
    }

    public void deleteAllVehicles() {
        vehicleRepository.deleteAllInBatch();
    }

    private <R, E extends AssignedIdEntity> void upsert(List<R> requests, Function<R, String> idOf, String idName,
                                                        JpaRepository<E, String> repository,
                                                        Function<R, E> create, BiConsumer<E, R> update) {
        List<String> ids = requests.stream().map(idOf).toList();
        rejectDuplicates(ids, idName);

        Map<String, E> existing = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(E::getId, Function.identity()));

        List<E> created = new ArrayList<>();
        for (R request : requests) {
            E entity = existing.get(idOf.apply(request));
            if (entity == null) {
                created.add(create.apply(request));
            } else {
                update.accept(entity, request);
            }
        }
        repository.saveAll(created);
    }

    private static void rejectDuplicates(List<String> ids, String idName) {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) {
                duplicates.add(id);
            }
        }
        if (!duplicates.isEmpty()) {
            throw new InvalidRequestException("Duplicate " + idName + " values in request",
                    duplicates.stream().map(id -> idName + " '" + id + "' appears more than once").toList());
        }
    }
}
