# Dispatch Load Balancer

A Spring Boot REST API that assigns delivery orders to a fleet of vehicles. It never exceeds a vehicle's capacity, assigns high-priority orders first, and keeps travel distance low by sending each order to the nearest vehicle that can carry it. Distances use the Haversine formula.

| | |
|---|---|
| **Live API** | `<LIVE_URL>` |
| **Swagger UI** | `<LIVE_URL>/swagger-ui.html` |
| **OpenAPI JSON** | `<LIVE_URL>/v3/api-docs` |
| **Health** | `<LIVE_URL>/actuator/health` |

> The live instance runs on Render's free tier. It sleeps after 15 minutes of inactivity, so the first request can take about 50 seconds.

## Tech stack

Java 21 · Spring Boot 4.1 · Spring Data JPA · H2 (in-memory) · Bean Validation · springdoc-openapi (Swagger) · JUnit 5, Mockito, MockMvc · JaCoCo · Docker

## API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/dispatch/orders` | Submit delivery orders (upsert by `orderId`) |
| `POST` | `/api/dispatch/vehicles` | Submit fleet details (upsert by `vehicleId`) |
| `GET` | `/api/dispatch/plan` | Compute and return the dispatch plan |
| `GET` | `/api/dispatch/orders` | List stored orders |
| `GET` | `/api/dispatch/vehicles` | List stored vehicles |
| `DELETE` | `/api/dispatch/orders` | Remove all orders |
| `DELETE` | `/api/dispatch/vehicles` | Remove all vehicles |

The first three endpoints follow the assignment specification. The others are extras for inspecting and resetting data during testing.

### Submit orders

```http
POST /api/dispatch/orders
{
  "orders": [
    { "orderId": "ORD001", "latitude": 28.6139, "longitude": 77.2090,
      "address": "Connaught Place, Delhi, India", "packageWeight": 15, "priority": "HIGH" }
  ]
}
```
```json
{ "message": "Delivery orders accepted.", "status": "success" }
```

### Submit vehicles

```http
POST /api/dispatch/vehicles
{
  "vehicles": [
    { "vehicleId": "VEH001", "capacity": 100, "currentLatitude": 28.7041,
      "currentLongitude": 77.1025, "currentAddress": "Karol Bagh, Delhi, India" }
  ]
}
```
```json
{ "message": "Vehicle details accepted.", "status": "success" }
```

### Get the dispatch plan

```http
GET /api/dispatch/plan
```
```json
{
  "dispatchPlan": [
    {
      "vehicleId": "VEH001",
      "totalLoad": 100,
      "totalDistance": "14.44 km",
      "assignedOrders": [
        { "orderId": "ORD026", "latitude": 28.7041, "longitude": 77.1025,
          "address": "Karol Bagh, Delhi, India", "packageWeight": 25, "priority": "HIGH" }
      ]
    }
  ],
  "unassignedOrders": [
    {
      "order": { "orderId": "ORD016", "latitude": 28.5355, "longitude": 77.391,
                 "address": "Sector 18, Noida, Uttar Pradesh, India", "packageWeight": 30, "priority": "LOW" },
      "reason": "No vehicle has enough remaining capacity for this package"
    }
  ]
}
```

- `assignedOrders` are listed in delivery sequence.
- `totalDistance` is measured from the vehicle's current location through every assigned stop.
- `unassignedOrders` is an addition to the spec. Without it, orders that cannot be dispatched would silently disappear from the response.

### Errors

Every error uses the same shape:

```json
{
  "status": "error",
  "code": 400,
  "message": "Validation failed",
  "errors": [
    "orders[0].latitude: must be less than or equal to 90.0",
    "orders[0].packageWeight: must be greater than 0"
  ],
  "timestamp": "2026-10-21T13:55:02.129Z"
}
```

| Case | Status |
|---|---|
| Missing or blank field, latitude outside ±90, longitude outside ±180, weight or capacity ≤ 0, empty list | 400 |
| Unknown priority (allowed: `HIGH`, `MEDIUM`, `LOW`, case-insensitive) | 400 |
| Same `orderId` / `vehicleId` twice in one request | 400 |
| Malformed JSON | 400 |
| Unknown path / wrong HTTP method / non-JSON body | 404 / 405 / 415 |
| Order heavier than every vehicle, fleet out of capacity, no vehicles | 200, listed in `unassignedOrders` with a reason |

## Dispatch algorithm

Implemented in [`PriorityGreedyDispatchStrategy`](src/main/java/com/freightfox/dispatch/strategy/PriorityGreedyDispatchStrategy.java).

1. **Sort orders** by priority (`HIGH`, then `MEDIUM`, then `LOW`). Within a priority, heavier packages come first, because large items are hardest to place once capacity runs low. `orderId` breaks remaining ties.
2. For each order, **consider only vehicles with enough remaining capacity** (`load + weight ≤ capacity`).
3. **Pick the nearest one.** Distance is measured with Haversine from the vehicle's *last stop*, which is its current location before it has any orders. This adds the smallest extra travel distance at each step. Equal distances are broken by `vehicleId`, so results are deterministic.
4. **Assign the order:** add its weight to the vehicle's load, add the leg to its distance, and move the vehicle's position to the order's location.
5. If no vehicle can take the order, **record it as unassigned** with one of these reasons:
   - no vehicles exist
   - the package is heavier than any vehicle's capacity
   - the fleet has run out of remaining capacity

The result satisfies the spec's constraints in priority order: capacity is a hard limit, high-priority orders are always served first, and distance is minimized greedily. Finding the exact optimum is the capacitated vehicle routing problem, which is NP-hard. This heuristic runs in **O(n log n + n·m)** for *n* orders and *m* vehicles; the test suite runs 2,000 orders across 20 vehicles in milliseconds.

Weights and capacities use `BigDecimal`, so fractional capacity checks are exact (0.1 + 0.2 fits into 0.3).

**On the sample data in the assignment:** total demand is 645 but fleet capacity is only 500. All HIGH and MEDIUM orders are assigned, 490 units of capacity are used, and the 8 remaining LOW orders are returned in `unassignedOrders`.

## Design

```
controller/   DispatchController (REST), RootController (/ → Swagger UI)
dto/          Request/response records with Bean Validation constraints
service/      DispatchService (transactions, upsert, orchestration), DispatchMapper (DTO ↔ entity)
strategy/     DispatchStrategy interface → PriorityGreedyDispatchStrategy
distance/     DistanceCalculator interface → HaversineDistanceCalculator
model/        DeliveryOrder, Vehicle (JPA entities), Priority
repository/   Spring Data JPA repositories
exception/    GlobalExceptionHandler, InvalidRequestException
config/       OpenAPI metadata
```

- **Strategy pattern.** Both the assignment algorithm (`DispatchStrategy`) and the distance metric (`DistanceCalculator`) sit behind interfaces. Replacing Haversine with a road-distance API, or the greedy heuristic with an optimization solver, is a new `@Component` with no other code changes.
- **Efficient upsert.** Existing rows are loaded with a single `IN` query and updated in place through dirty checking. New rows implement `Persistable`, so they are batch-inserted without the per-row `SELECT` that Spring Data normally runs for entities with client-assigned ids. JDBC batching is enabled.
- **Clear layer boundaries.** The controllers only deal with DTOs, and the strategy works on plain domain objects. Neither knows about HTTP or the database.
- **Storage.** Data is held in-memory in H2, so it resets on restart. Moving to PostgreSQL only requires a datasource change.

## Running locally

Requires **JDK 21**. You do not need Maven installed, because the Maven wrapper is included.

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Then open http://localhost:8080/swagger-ui.html.

With Docker:

```bash
docker build -t dispatch-load-balancer .
docker run -p 8080:8080 dispatch-load-balancer
```

### Try it with the assignment's sample data

```bash
curl -X POST localhost:8080/api/dispatch/vehicles -H "Content-Type: application/json" -d @src/main/resources/examples/sample-vehicles.json
curl -X POST localhost:8080/api/dispatch/orders   -H "Content-Type: application/json" -d @src/main/resources/examples/sample-orders.json
curl localhost:8080/api/dispatch/plan
```

A Postman collection with the happy path, edge cases and reset requests is in [`postman/`](postman/dispatch-load-balancer.postman_collection.json). Set its `baseUrl` variable to your local or live URL.

## Tests

```bash
./mvnw test
```

There are 56 tests, with about 97% line coverage and 97% branch coverage. The HTML coverage report is generated at `target/site/jacoco/index.html`.

| Test class | What it covers |
|---|---|
| `HaversineDistanceCalculatorTest` | Known distances (Delhi to Mumbai, across the antimeridian, antipodal points), zero distance, symmetry |
| `PriorityGreedyDispatchStrategyTest` | Nearest-vehicle choice, priority before distance, heavier packages first, overflow to the next vehicle, filling exactly to capacity, oversized packages, no vehicles, no orders, distance accumulated across stops, deterministic tie-breaks, fractional weights, and a 2,000-order randomized check that capacity is never exceeded and every order is accounted for |
| `DispatchServiceTest` | Upsert behaviour (update vs. insert), duplicate-id rejection, id trimming, delegation to the strategy |
| `DispatchControllerTest` | Request validation, error format, 400/405/415 responses, response shape |
| `DispatchIntegrationTest` | The full flow on the assignment's sample data, upsert through the API, empty states, 404s, Swagger and health endpoints |

## Deployment

The app is deployed on Render as a Docker web service using [`render.yaml`](render.yaml), with the health check at `/actuator/health`. Render supplies the port through the `PORT` environment variable. `server.forward-headers-strategy=framework` makes Swagger generate `https` URLs behind Render's proxy.
