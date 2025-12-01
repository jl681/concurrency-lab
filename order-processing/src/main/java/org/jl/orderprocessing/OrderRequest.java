package org.jl.orderprocessing;

import java.util.UUID;

public record OrderRequest(
        UUID userId,
        Long productId,
        Integer quantity,
        Double price,
        String shippingAddress
) {
    // You can add validation logic inside the compact constructor
    public OrderRequest {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}