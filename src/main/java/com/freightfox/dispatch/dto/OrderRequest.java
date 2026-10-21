package com.freightfox.dispatch.dto;

import com.freightfox.dispatch.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderRequest(
        @Schema(example = "ORD001")
        @NotBlank @Size(max = 64)
        String orderId,

        @Schema(example = "28.6139")
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @Schema(example = "77.2090")
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude,

        @Schema(example = "Connaught Place, Delhi, India")
        @NotBlank @Size(max = 255)
        String address,

        @Schema(example = "15", description = "Package weight, in the same unit as vehicle capacity")
        @NotNull @Positive @Digits(integer = 9, fraction = 3)
        BigDecimal packageWeight,

        @Schema(example = "HIGH", description = "HIGH, MEDIUM or LOW (case-insensitive)")
        @NotNull
        Priority priority) {
}
