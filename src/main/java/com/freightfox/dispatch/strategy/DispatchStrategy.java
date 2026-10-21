package com.freightfox.dispatch.strategy;

import com.freightfox.dispatch.model.DeliveryOrder;
import com.freightfox.dispatch.model.Vehicle;

import java.util.List;

public interface DispatchStrategy {

    DispatchResult plan(List<DeliveryOrder> orders, List<Vehicle> vehicles);
}
