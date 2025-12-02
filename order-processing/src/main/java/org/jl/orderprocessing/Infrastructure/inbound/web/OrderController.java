package org.jl.orderprocessing.Infrastructure.inbound.web;

import org.jl.orderprocessing.Infrastructure.inbound.web.dto.OrderRequest;
import org.jl.orderprocessing.application.service.OrderOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderOrchestratorService orchestrator;

    public OrderController(OrderOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<String>> createOrder(@RequestBody OrderRequest request) {
        // We offload this to a Virtual Thread immediately
        return CompletableFuture.supplyAsync(() -> {
            orchestrator.processOrder(request);
            return ResponseEntity.accepted().body("Order Processing Started");
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
}