package com.freightfox.dispatch.model;

import com.freightfox.dispatch.distance.GeoPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "delivery_orders")
public class DeliveryOrder extends AssignedIdEntity {

    @Id
    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private String address;

    @Column(name = "package_weight", nullable = false, precision = 12, scale = 3)
    private BigDecimal packageWeight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    protected DeliveryOrder() {
    }

    public DeliveryOrder(String orderId, double latitude, double longitude, String address,
                         BigDecimal packageWeight, Priority priority) {
        this.orderId = orderId;
        update(latitude, longitude, address, packageWeight, priority);
    }

    public void update(double latitude, double longitude, String address, BigDecimal packageWeight, Priority priority) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.packageWeight = packageWeight;
        this.priority = priority;
    }

    public GeoPoint location() {
        return new GeoPoint(latitude, longitude);
    }

    @Override
    public String getId() {
        return orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getPackageWeight() {
        return packageWeight;
    }

    public Priority getPriority() {
        return priority;
    }
}
