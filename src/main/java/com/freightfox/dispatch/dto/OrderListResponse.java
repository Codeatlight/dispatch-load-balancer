package com.freightfox.dispatch.dto;

import java.util.List;

public record OrderListResponse(List<OrderResponse> orders) {
}
