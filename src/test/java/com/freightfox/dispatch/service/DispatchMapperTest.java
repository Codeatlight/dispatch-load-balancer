package com.freightfox.dispatch.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchMapperTest {

    @ParameterizedTest
    @CsvSource({"10.000, 10", "100.000, 100", "12.500, 12.5", "0.125, 0.125"})
    void normalizeDropsDatabaseScalePadding(String stored, String expected) {
        assertThat(DispatchMapper.normalize(new BigDecimal(stored)).toString()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, 0.00 km", "5, 5.00 km", "12.345, 12.35 km", "1148.0964, 1148.10 km"})
    void formatsDistanceInKilometres(double km, String expected) {
        assertThat(DispatchMapper.formatKm(km)).isEqualTo(expected);
    }
}
