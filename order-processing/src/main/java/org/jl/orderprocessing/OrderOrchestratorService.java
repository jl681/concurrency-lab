package org.jl.orderprocessing;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@AllArgsConstructor
public class OrderOrchestratorService {

    private final OrderRepository orderRepo;
    private final NotificationClient notificationClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRepository outboxRepo; // For Kafka resilience

    // Java 21: Virtual Thread Executor for high concurrency I/O
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public void processOrder(OrderRequest request) {
        // STEP 1: Local ACID Transaction (Fast)
        // Save as PENDING. Commit immediately to release DB Lock.
        Order order = saveInitialOrder(request);

        try {
            // STEP 2: Parallel API Calls (The "5 APIs")
            // We fan-out to call all 5 services at once using Virtual Threads
            var apiFutures = List.of(
                    runApiCall(() -> notificationClient.notifyInventory(order)),
                    runApiCall(() -> notificationClient.notifyLogistics(order)),
                    runApiCall(() -> notificationClient.notifyAnalytics(order)),
                    runApiCall(() -> notificationClient.notifyCRM(order)),
                    runApiCall(() -> notificationClient.notifyVendor(order))
            );

            // Wait for ALL to finish (or throw exception)
            CompletableFuture.allOf(apiFutures.toArray(new CompletableFuture[0])).join();

            // STEP 3: Publish to Kafka (Resilient)
            publishEventSafe(order);

            // STEP 4: Success - Update DB to CONFIRMED
            updateOrderStatus(order, OrderStatus.CONFIRMED);

        } catch (Exception e) {
            log.error("Order processing failed. Starting Rollback...", e);
            // STEP 5: THE ROLLBACK (Compensating Transaction)
            handleFailureAndRollback(order);
        }
    }

    // --- Helper Methods ---

    // Wraps execution in CompletableFuture running on Virtual Thread
    private CompletableFuture<Void> runApiCall(Runnable task) {
        return CompletableFuture.runAsync(task, ioExecutor);
    }

    @Transactional
    protected Order saveInitialOrder(OrderRequest req) {
        Order order = new Order(req);
        order.setStatus(OrderStatus.PENDING);
        return orderRepo.save(order);
    }

    @Transactional
    protected void updateOrderStatus(Order order, OrderStatus status) {
        order.setStatus(status);
        orderRepo.save(order);
    }

    // --- Resilience Logic ---

    /**
     * Resilient Kafka Publisher (Outbox Pattern Lite)
     * If Kafka is down, we save to a local DB table to retry later.
     */
    private void publishEventSafe(Order order) {
        try {
            kafkaTemplate.send("orders-topic", order.getId().toString(), "ORDER_CREATED")
                    .get(2, TimeUnit.SECONDS); // Timeout fast!
        } catch (Exception e) {
            log.warn("Kafka is down! Saving to Outbox table for retry.");
            saveToOutbox(order);
        }
    }

    @Transactional
    protected void saveToOutbox(Order order) {
        // A background cron job will pick this up and retry sending to Kafka later
        outboxRepo.save(new OutboxEvent(
                "orders-topic",
                order.getId().toString(),
                "ORDER_CREATED"
        ));
    }

    /**
     * THE ROLLBACK LOGIC
     * Since we can't "undo" an HTTP request, we mark the local data as Failed
     * and potentially send "Undo" requests to external services.
     */
    private void handleFailureAndRollback(Order order) {
        // 1. Mark local DB as FAILED
        updateOrderStatus(order, OrderStatus.FAILED);

        // 2. (Optional) Semantic Rollback
        // If "notifyInventory" reserved items, we must call "releaseInventory"
        // This is done asynchronously so we don't block the user further.
        CompletableFuture.runAsync(() -> {
            notificationClient.compensateInventory(order);
        }, ioExecutor);
    }
}
