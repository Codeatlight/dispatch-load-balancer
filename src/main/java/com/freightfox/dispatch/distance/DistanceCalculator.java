package com.freightfox.dispatch.distance;

public interface DistanceCalculator {

    double distanceKm(GeoPoint from, GeoPoint to);
}
