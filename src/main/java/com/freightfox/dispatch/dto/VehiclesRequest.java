package com.freightfox.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VehiclesRequest(
        @NotEmpty @Size(max = VehiclesRequest.MAX_BATCH_SIZE)
        List<@Valid @NotNull VehicleRequest> vehicles) {

    public static final int MAX_BATCH_SIZE = 1_000;
}
