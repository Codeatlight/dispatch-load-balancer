package com.freightfox.dispatch;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Priority;
import com.freightfox.dispatch.model.Vehicle;

import java.math.BigDecimal;

public final class TestData {

    public static final double CP_LAT = 28.6139;
    public static final double CP_LON = 77.2090;
    public static final double NOIDA_LAT = 28.5355;
    public static final double NOIDA_LON = 77.3910;
    public static final double GURGAON_LAT = 28.4595;
    public static final double GURGAON_LON = 77.0266;

    private TestData() {
    }

    public static DeliveryOrder order(String id, double lat, double lon, int weight, Priority priority) {
        return new DeliveryOrder(id, lat, lon, "Address of " + id, BigDecimal.valueOf(weight), priority);
    }

    public static Vehicle vehicle(String id, int capacity, double lat, double lon) {
        return new Vehicle(id, BigDecimal.valueOf(capacity), lat, lon, "Address of " + id);
    }
}
