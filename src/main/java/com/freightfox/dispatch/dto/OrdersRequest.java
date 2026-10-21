package com.freightfox.dispatch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrdersRequest(
        @NotEmpty @Size(max = OrdersRequest.MAX_BATCH_SIZE)
        List<@Valid @NotNull OrderRequest> orders) {

    public static final int MAX_BATCH_SIZE = 10_000;
}
