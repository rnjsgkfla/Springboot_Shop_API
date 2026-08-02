package com.skala.shop.dto;

import com.skala.shop.entity.Customer;

public record CustomerResponse(
    String customerId,
    Double customerPoint,
    String referrerId
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getCustomerId(),
            customer.getCustomerPoint(),
            customer.getReferrer() == null ? null : customer.getReferrer().getCustomerId()
        );
    }
}
