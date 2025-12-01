package org.jl.orderprocessing;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
public class NotificationClient {

    private final RestTemplate restTemplate;

    // Inventory
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "inventoryFallback")
    @TimeLimiter(name = "inventoryService")
    public CompletableFuture<Void> notifyInventory(Order order) {
        return CompletableFuture.runAsync(() ->
                restTemplate.postForEntity("http://inventory-service/reserve", order, Void.class)
        );
    }

    public CompletableFuture<Void> inventoryFallback(Order order, Throwable t) {
        return CompletableFuture.failedFuture(new RuntimeException("Inventory Service Unavailable - Rollback Order", t));
    }

    public void compensateInventory(Order order) {
        restTemplate.postForEntity("http://inventory-service/undo", order, Void.class);
    }

    // Logistics
    @CircuitBreaker(name = "logisticsService", fallbackMethod = "logisticsFallback")
    @TimeLimiter(name = "logisticsService")
    public CompletableFuture<Void> notifyLogistics(Order order) {
        return CompletableFuture.runAsync(() ->
                restTemplate.postForEntity("http://logistics-service/schedule", order, Void.class)
        );
    }

    public CompletableFuture<Void> logisticsFallback(Order order, Throwable t) {
        return CompletableFuture.failedFuture(new RuntimeException("Logistics Service Unavailable - Rollback Order", t));
    }

    public void compensateLogistics(Order order) {
        restTemplate.postForEntity("http://logistics-service/undo", order, Void.class);
    }

    // Analytics
    @CircuitBreaker(name = "analyticsService", fallbackMethod = "analyticsFallback")
    @TimeLimiter(name = "analyticsService")
    public CompletableFuture<Void> notifyAnalytics(Order order) {
        return CompletableFuture.runAsync(() ->
                restTemplate.postForEntity("http://analytics-service/track", order, Void.class)
        );
    }

    public CompletableFuture<Void> analyticsFallback(Order order, Throwable t) {
        return CompletableFuture.failedFuture(new RuntimeException("Analytics Service Unavailable - Rollback Order", t));
    }

    public void compensateAnalytics(Order order) {
        restTemplate.postForEntity("http://analytics-service/undo", order, Void.class);
    }

    // CRM
    @CircuitBreaker(name = "crmService", fallbackMethod = "crmFallback")
    @TimeLimiter(name = "crmService")
    public CompletableFuture<Void> notifyCRM(Order order) {
        return CompletableFuture.runAsync(() ->
                restTemplate.postForEntity("http://crm-service/notify", order, Void.class)
        );
    }

    public CompletableFuture<Void> crmFallback(Order order, Throwable t) {
        return CompletableFuture.failedFuture(new RuntimeException("CRM Service Unavailable - Rollback Order", t));
    }

    public void compensateCRM(Order order) {
        restTemplate.postForEntity("http://crm-service/undo", order, Void.class);
    }

    // Vendor
    @CircuitBreaker(name = "vendorService", fallbackMethod = "vendorFallback")
    @TimeLimiter(name = "vendorService")
    public CompletableFuture<Void> notifyVendor(Order order) {
        return CompletableFuture.runAsync(() ->
                restTemplate.postForEntity("http://vendor-service/order", order, Void.class)
        );
    }

    public CompletableFuture<Void> vendorFallback(Order order, Throwable t) {
        return CompletableFuture.failedFuture(new RuntimeException("Vendor Service Unavailable - Rollback Order", t));
    }

    public void compensateVendor(Order order) {
        restTemplate.postForEntity("http://vendor-service/undo", order, Void.class);
    }
}
