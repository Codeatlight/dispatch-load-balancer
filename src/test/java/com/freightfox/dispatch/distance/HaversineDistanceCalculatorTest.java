package com.freightfox.dispatch.distance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void samePointIsZeroDistance() {
        GeoPoint point = new GeoPoint(28.6139, 77.2090);

        assertThat(calculator.distanceKm(point, point)).isZero();
    }

    @ParameterizedTest(name = "{4}")
    @CsvSource({
            "28.6139, 77.2090, 19.0760,  72.8777, Delhi to Mumbai,           1148.10",
            "28.6139, 77.2090, 28.5355,  77.3910, Connaught Place to Noida,  19.80",
            "0,       0,       1,        0,       One degree of latitude,    111.20",
            "0,       179.5,   0,        -179.5,  Across the antimeridian,   111.20",
            "0,       0,       0,        180,     Antipodal points,          20015.11"
    })
    void matchesKnownDistances(double lat1, double lon1, double lat2, double lon2, String name, double expectedKm) {
        double actual = calculator.distanceKm(new GeoPoint(lat1, lon1), new GeoPoint(lat2, lon2));

        assertThat(actual).isCloseTo(expectedKm, within(0.01));
    }

    @Test
    void isSymmetric() {
        GeoPoint delhi = new GeoPoint(28.6139, 77.2090);
        GeoPoint chennai = new GeoPoint(13.0827, 80.2707);

        assertThat(calculator.distanceKm(delhi, chennai))
                .isCloseTo(calculator.distanceKm(chennai, delhi), within(1e-9));
    }
}
