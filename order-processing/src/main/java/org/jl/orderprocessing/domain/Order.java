package org.jl.orderprocessing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jl.orderprocessing.Infrastructure.inbound.web.dto.OrderRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders") // Maps to the "orders" table in Postgres
@Getter @Setter
@NoArgsConstructor // JPA requires a no-args constructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // We copy data from Request to Entity to decouple the API from the DB
    private UUID userId;
    private Long productId;
    private Integer quantity;
    private Double price;

    @Enumerated(EnumType.STRING) // Saves as text ("PENDING") instead of number (0)
    private OrderStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- CONCURRENCY SAFETY ---
    // This magic field prevents "Lost Updates" (Race Conditions)
    // JPA increments this number every time you save.
    // If the version in DB != version in memory, the save fails.
    @Version
    private Long version;

    // Constructor to create from Request
    public Order(OrderRequest req) {
        this.userId = req.userId();
        this.productId = req.productId();
        this.quantity = req.quantity();
        this.price = req.price();
        this.status = OrderStatus.PENDING; // Default state
        this.createdAt = LocalDateTime.now();
    }
}
