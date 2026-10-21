package com.freightfox.dispatch.model;

import com.freightfox.dispatch.distance.GeoPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
public class Vehicle extends AssignedIdEntity {

    @Id
    @Column(name = "vehicle_id", length = 64)
    private String vehicleId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal capacity;

    @Column(name = "current_latitude", nullable = false)
    private double currentLatitude;

    @Column(name = "current_longitude", nullable = false)
    private double currentLongitude;

    @Column(name = "current_address", nullable = false)
    private String currentAddress;

    protected Vehicle() {
    }

    public Vehicle(String vehicleId, BigDecimal capacity, double currentLatitude, double currentLongitude,
                   String currentAddress) {
        this.vehicleId = vehicleId;
        update(capacity, currentLatitude, currentLongitude, currentAddress);
    }

    public void update(BigDecimal capacity, double currentLatitude, double currentLongitude, String currentAddress) {
        this.capacity = capacity;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.currentAddress = currentAddress;
    }

    public GeoPoint location() {
        return new GeoPoint(currentLatitude, currentLongitude);
    }

    @Override
    public String getId() {
        return vehicleId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public BigDecimal getCapacity() {
        return capacity;
    }

    public double getCurrentLatitude() {
        return currentLatitude;
    }

    public double getCurrentLongitude() {
        return currentLongitude;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }
}
