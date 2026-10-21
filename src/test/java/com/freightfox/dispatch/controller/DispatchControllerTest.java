package com.freightfox.dispatch.controller;

import com.freightfox.dispatch.dto.DispatchPlanResponse;
import com.freightfox.dispatch.dto.OrderResponse;
import com.freightfox.dispatch.dto.UnassignedOrderResponse;
import com.freightfox.dispatch.dto.VehiclePlanResponse;
import com.freightfox.dispatch.exception.InvalidRequestException;
import com.freightfox.dispatch.model.Priority;
import com.freightfox.dispatch.service.DispatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DispatchController.class)
class DispatchControllerTest {

    private static final String VALID_ORDER = """
            {"orderId": "ORD001", "latitude": 28.6139, "longitude": 77.2090,
             "address": "Connaught Place, Delhi, India", "packageWeight": 15, "priority": "HIGH"}""";

    private static final String VALID_VEHICLE = """
            {"vehicleId": "VEH001", "capacity": 100, "currentLatitude": 28.7041, "currentLongitude": 77.1025,
             "currentAddress": "Karol Bagh, Delhi, India"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispatchService dispatchService;

    @Test
    void acceptsValidOrders() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\": [" + VALID_ORDER + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delivery orders accepted."))
                .andExpect(jsonPath("$.status").value("success"));

        verify(dispatchService).saveOrders(anyList());
    }

    @Test
    void acceptsPriorityCaseInsensitively() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\": [" + VALID_ORDER.replace("\"HIGH\"", "\"high\"") + "]}")
                .andExpect(status().isOk());
    }

    @Test
    void acceptsValidVehicles() throws Exception {
        postJson("/api/dispatch/vehicles", "{\"vehicles\": [" + VALID_VEHICLE + "]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vehicle details accepted."))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void reportsEveryMissingOrderField() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\": [{}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(6))
                .andExpect(jsonPath("$.errors", hasItem("orders[0].orderId: must not be blank")))
                .andExpect(jsonPath("$.errors", hasItem("orders[0].priority: must not be null")));
        verifyNoInteractions(dispatchService);
    }

    @Test
    void rejectsOutOfRangeCoordinatesAndNonPositiveWeight() throws Exception {
        String order = VALID_ORDER.replace("28.6139", "91").replace("77.2090", "-181").replace("15", "0");

        postJson("/api/dispatch/orders", "{\"orders\": [" + order + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(containsString("orders[0].latitude"))))
                .andExpect(jsonPath("$.errors", hasItem(containsString("orders[0].longitude"))))
                .andExpect(jsonPath("$.errors", hasItem("orders[0].packageWeight: must be greater than 0")));
    }

    @Test
    void rejectsEmptyOrderList() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\": []}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(containsString("orders: must not be empty"))));
    }

    @Test
    void rejectsUnknownPriority() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\": [" + VALID_ORDER.replace("HIGH", "URGENT") + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.errors[0]").value(containsString("Invalid priority 'URGENT'")));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\": [")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void rejectsNonPositiveVehicleCapacity() throws Exception {
        postJson("/api/dispatch/vehicles", "{\"vehicles\": [" + VALID_VEHICLE.replace("100", "-5") + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem("vehicles[0].capacity: must be greater than 0")));
    }

    @Test
    void returnsBadRequestForDuplicateIds() throws Exception {
        doThrow(new InvalidRequestException("Duplicate orderId values in request",
                List.of("orderId 'ORD001' appears more than once")))
                .when(dispatchService).saveOrders(anyList());

        postJson("/api/dispatch/orders", "{\"orders\": [" + VALID_ORDER + "," + VALID_ORDER + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Duplicate orderId values in request"))
                .andExpect(jsonPath("$.errors[0]").value("orderId 'ORD001' appears more than once"));
    }

    @Test
    void rejectsNonJsonContentType() throws Exception {
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(415));
    }

    @Test
    void rejectsUnsupportedMethod() throws Exception {
        mockMvc.perform(put("/api/dispatch/plan"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    void returnsDispatchPlan() throws Exception {
        OrderResponse order = new OrderResponse("ORD001", 28.6139, 77.2090, "Connaught Place, Delhi, India",
                new BigDecimal("10"), Priority.HIGH);
        when(dispatchService.getDispatchPlan()).thenReturn(new DispatchPlanResponse(
                List.of(new VehiclePlanResponse("VEH001", new BigDecimal("10"), "5.00 km", List.of(order))),
                List.of(new UnassignedOrderResponse(order, "Package weight exceeds the capacity of every vehicle"))));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan[0].vehicleId").value("VEH001"))
                .andExpect(jsonPath("$.dispatchPlan[0].totalLoad").value(10))
                .andExpect(jsonPath("$.dispatchPlan[0].totalDistance").value("5.00 km"))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.unassignedOrders[0].reason").value(containsString("exceeds")));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
