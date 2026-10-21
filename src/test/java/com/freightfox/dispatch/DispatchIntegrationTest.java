package com.freightfox.dispatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DispatchIntegrationTest {

    private static final Path SAMPLE_ORDERS = Path.of("src/main/resources/examples/sample-orders.json");
    private static final Path SAMPLE_VEHICLES = Path.of("src/main/resources/examples/sample-vehicles.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearData() throws Exception {
        mockMvc.perform(delete("/api/dispatch/orders")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/dispatch/vehicles")).andExpect(status().isOk());
    }

    @Test
    void producesAValidPlanForTheAssignmentSampleData() throws Exception {
        postJson("/api/dispatch/vehicles", Files.readString(SAMPLE_VEHICLES));
        postJson("/api/dispatch/orders", Files.readString(SAMPLE_ORDERS));

        JsonNode plan = getJson("/api/dispatch/plan");
        JsonNode inputOrders = objectMapper.readTree(Files.readString(SAMPLE_ORDERS)).get("orders");
        Map<String, BigDecimal> capacities = new HashMap<>();
        objectMapper.readTree(Files.readString(SAMPLE_VEHICLES)).get("vehicles")
                .forEach(v -> capacities.put(v.get("vehicleId").asString(), v.get("capacity").decimalValue()));

        Set<String> accountedFor = new HashSet<>();
        assertThat(plan.get("dispatchPlan")).hasSize(5);
        for (JsonNode vehicle : plan.get("dispatchPlan")) {
            BigDecimal load = BigDecimal.ZERO;
            for (JsonNode order : vehicle.get("assignedOrders")) {
                load = load.add(order.get("packageWeight").decimalValue());
                assertThat(accountedFor.add(order.get("orderId").asString())).isTrue();
            }
            assertThat(vehicle.get("totalLoad").decimalValue()).isEqualByComparingTo(load);
            assertThat(load).isLessThanOrEqualTo(capacities.get(vehicle.get("vehicleId").asString()));
            assertThat(vehicle.get("totalDistance").asString()).matches("\\d+\\.\\d{2} km");
        }
        for (JsonNode unassigned : plan.get("unassignedOrders")) {
            assertThat(unassigned.get("order").get("priority").asString()).isEqualTo("LOW");
            assertThat(unassigned.get("reason").asString()).isNotBlank();
            assertThat(accountedFor.add(unassigned.get("order").get("orderId").asString())).isTrue();
        }
        assertThat(plan.get("unassignedOrders")).isNotEmpty();
        assertThat(accountedFor).hasSize(inputOrders.size());
    }

    @Test
    void resubmittingAVehicleUpdatesItInsteadOfDuplicating() throws Exception {
        postJson("/api/dispatch/vehicles", """
                {"vehicles": [{"vehicleId": "VEH001", "capacity": 100, "currentLatitude": 28.7041,
                  "currentLongitude": 77.1025, "currentAddress": "Karol Bagh, Delhi, India"}]}""");
        postJson("/api/dispatch/vehicles", """
                {"vehicles": [{"vehicleId": "VEH001", "capacity": 150, "currentLatitude": 28.6139,
                  "currentLongitude": 77.2090, "currentAddress": "Connaught Place, Delhi, India"}]}""");

        mockMvc.perform(get("/api/dispatch/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicles.length()").value(1))
                .andExpect(jsonPath("$.vehicles[0].capacity").value(150))
                .andExpect(jsonPath("$.vehicles[0].currentAddress").value("Connaught Place, Delhi, India"));
    }

    @Test
    void resubmittingAnOrderUpdatesItInsteadOfDuplicating() throws Exception {
        String order = """
                {"orders": [{"orderId": "ORD001", "latitude": 28.6139, "longitude": 77.2090,
                  "address": "Connaught Place, Delhi, India", "packageWeight": %s, "priority": "%s"}]}""";
        postJson("/api/dispatch/orders", order.formatted("10", "LOW"));
        postJson("/api/dispatch/orders", order.formatted("12.5", "HIGH"));

        mockMvc.perform(get("/api/dispatch/orders"))
                .andExpect(jsonPath("$.orders.length()").value(1))
                .andExpect(jsonPath("$.orders[0].packageWeight").value(12.5))
                .andExpect(jsonPath("$.orders[0].priority").value("HIGH"));
    }

    @Test
    void returnsEmptyPlanWhenNothingIsStored() throws Exception {
        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan.length()").value(0))
                .andExpect(jsonPath("$.unassignedOrders.length()").value(0));
    }

    @Test
    void reportsAllOrdersUnassignedWhenThereIsNoFleet() throws Exception {
        postJson("/api/dispatch/orders", Files.readString(SAMPLE_ORDERS));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(jsonPath("$.dispatchPlan.length()").value(0))
                .andExpect(jsonPath("$.unassignedOrders.length()").value(30))
                .andExpect(jsonPath("$.unassignedOrders[0].reason").value("No vehicles are available in the fleet"));
    }

    @Test
    void returnsJsonErrorForUnknownPath() throws Exception {
        mockMvc.perform(get("/api/dispatch/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void rootRedirectsToSwaggerUi() throws Exception {
        mockMvc.perform(get("/")).andExpect(redirectedUrl("/swagger-ui.html"));
    }

    @Test
    void publishesOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Dispatch Load Balancer API"))
                .andExpect(jsonPath("$.paths['/api/dispatch/plan']").exists())
                .andExpect(jsonPath("$.components.examples['sample-orders'].value.orders.length()").value(30));
    }

    @Test
    void exposesHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private void postJson(String path, String body) throws Exception {
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private JsonNode getJson(String path) throws Exception {
        String body = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
