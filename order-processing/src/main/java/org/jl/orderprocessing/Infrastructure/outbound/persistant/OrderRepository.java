package org.jl.orderprocessing.Infrastructure.outbound.persistant;


import org.jl.orderprocessing.domain.Order;
import org.jl.orderprocessing.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Spring Data Magic: This automatically generates:
    // SELECT * FROM orders WHERE user_id = ?
    List<Order> findByUserId(UUID userId);

    // Useful for finding "stuck" orders (e.g., PENDING for > 1 hour)
    List<Order> findByStatus(OrderStatus status);
}
