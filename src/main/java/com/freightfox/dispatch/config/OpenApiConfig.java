package com.freightfox.dispatch.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final Map<String, String> EXAMPLES = Map.of(
            "sample-orders", "Assignment sample data: 30 orders",
            "sample-vehicles", "Assignment sample data: 5 vehicles",
            "updated-vehicle", "Resubmit VEH002 with a larger capacity (upsert)",
            "oversized-order", "Order heavier than every vehicle",
            "invalid-orders", "Invalid fields (400)",
            "invalid-priority", "Unknown priority (400)",
            "duplicate-orders", "Duplicate orderId (400)",
            "invalid-vehicles", "Invalid vehicle fields (400)");

    @Bean
    public OpenAPI dispatchOpenApi() {
        Components components = new Components();
        ObjectMapper mapper = new ObjectMapper();
        EXAMPLES.forEach((name, summary) ->
                components.addExamples(name, new Example().summary(summary).value(readExample(mapper, name))));

        return new OpenAPI()
                .components(components)
                .info(new Info()
                        .title("Dispatch Load Balancer API")
                        .version("1.0.0")
                        .description("""
                                Assigns delivery orders to a fleet of vehicles.

                                **Typical flow:** `POST /api/dispatch/vehicles`, then `POST /api/dispatch/orders`, \
                                then `GET /api/dispatch/plan`. Each POST offers ready-made bodies in its **Examples** \
                                dropdown, including the assignment's sample data.

                                Rules: vehicle capacity is never exceeded, HIGH priority orders are assigned before \
                                MEDIUM and LOW, and each order goes to the nearest vehicle (Haversine distance from \
                                its last stop) that can still carry it. Orders that cannot be carried are returned \
                                in `unassignedOrders` with a reason."""));
    }

    private static Object readExample(ObjectMapper mapper, String name) {
        try (InputStream in = new ClassPathResource("examples/" + name + ".json").getInputStream()) {
            return mapper.readValue(in, Map.class);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot load OpenAPI example " + name, ex);
        }
    }
}
