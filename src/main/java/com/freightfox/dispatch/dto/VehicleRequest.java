package com.freightfox.dispatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record VehicleRequest(
        @Schema(example = "VEH001")
        @NotBlank @Size(max = 64)
        String vehicleId,

        @Schema(example = "100")
        @NotNull @Positive @Digits(integer = 9, fraction = 3)
        BigDecimal capacity,

        @Schema(example = "28.7041")
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double currentLatitude,

        @Schema(example = "77.1025")
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double currentLongitude,

        @Schema(example = "Karol Bagh, Delhi, India")
        @NotBlank @Size(max = 255)
        String currentAddress) {
}
