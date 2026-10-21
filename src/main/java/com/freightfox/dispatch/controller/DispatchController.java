package com.freightfox.dispatch.controller;

import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.ErrorResponse;
import com.freightfox.dispatch.dto.OrderListResponse;
import com.freightfox.dispatch.dto.OrdersRequest;
import com.freightfox.dispatch.dto.StatusResponse;
import com.freightfox.dispatch.dto.VehicleListResponse;
import com.freightfox.dispatch.dto.VehiclesRequest;
import com.freightfox.dispatch.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/dispatch", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dispatch", description = "Submit delivery orders and fleet details, then retrieve the optimized dispatch plan")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping(path = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit delivery orders",
            description = "Adds the orders. An order whose orderId already exists is updated instead of duplicated.")
    @ApiResponse(responseCode = "200", description = "Orders accepted")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = OrdersRequest.class),
            examples = {
                    @ExampleObject(name = "Assignment sample data: 30 orders", ref = "#/components/examples/sample-orders"),
                    @ExampleObject(name = "Order heavier than every vehicle", ref = "#/components/examples/oversized-order"),
                    @ExampleObject(name = "Invalid fields (400)", ref = "#/components/examples/invalid-orders"),
                    @ExampleObject(name = "Unknown priority (400)", ref = "#/components/examples/invalid-priority"),
                    @ExampleObject(name = "Duplicate orderId (400)", ref = "#/components/examples/duplicate-orders")
            }))
    public StatusResponse submitOrders(@Valid @RequestBody OrdersRequest request) {
        dispatchService.saveOrders(request.orders());
        return StatusResponse.success("Delivery orders accepted.");
    }

    @PostMapping(path = "/vehicles", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit fleet details",
            description = "Adds the vehicles. A vehicle whose vehicleId already exists is updated instead of duplicated.")
    @ApiResponse(responseCode = "200", description = "Vehicles accepted")
    @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = VehiclesRequest.class),
            examples = {
                    @ExampleObject(name = "Assignment sample data: 5 vehicles", ref = "#/components/examples/sample-vehicles"),
                    @ExampleObject(name = "Resubmit VEH002 with a larger capacity", ref = "#/components/examples/updated-vehicle"),
                    @ExampleObject(name = "Invalid vehicle fields (400)", ref = "#/components/examples/invalid-vehicles")
            }))
    public StatusResponse submitVehicles(@Valid @RequestBody VehiclesRequest request) {
        dispatchService.saveVehicles(request.vehicles());
        return StatusResponse.success("Vehicle details accepted.");
    }

    @GetMapping("/plan")
    @Operation(summary = "Retrieve the dispatch plan",
            description = "Assigns all stored orders to the stored vehicles: HIGH priority first, never exceeding "
                    + "capacity, always choosing the nearest vehicle (Haversine) that can carry the package. "
                    + "Orders that cannot be carried are listed in unassignedOrders with a reason.")
    @ApiResponse(responseCode = "200", description = "Dispatch plan")
    public DispatchPlanResponse getDispatchPlan() {
        return dispatchService.getDispatchPlan();
    }

    @GetMapping("/orders")
    @Operation(summary = "List stored delivery orders")
    public OrderListResponse getOrders() {
        return dispatchService.getOrders();
    }

    @GetMapping("/vehicles")
    @Operation(summary = "List stored vehicles")
    public VehicleListResponse getVehicles() {
        return dispatchService.getVehicles();
    }

    @DeleteMapping("/orders")
    @Operation(summary = "Remove all delivery orders")
    public StatusResponse deleteOrders() {
        dispatchService.deleteAllOrders();
        return StatusResponse.success("All delivery orders removed.");
    }

    @DeleteMapping("/vehicles")
    @Operation(summary = "Remove all vehicles")
    public StatusResponse deleteVehicles() {
        dispatchService.deleteAllVehicles();
        return StatusResponse.success("All vehicles removed.");
    }
}
