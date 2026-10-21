package com.freightfox.dispatch.strategy;

public enum UnassignedReason {
    NO_VEHICLES("No vehicles are available in the fleet"),
    EXCEEDS_MAX_VEHICLE_CAPACITY("Package weight exceeds the capacity of every vehicle"),
    INSUFFICIENT_REMAINING_CAPACITY("No vehicle has enough remaining capacity for this package");

    private final String description;

    UnassignedReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
