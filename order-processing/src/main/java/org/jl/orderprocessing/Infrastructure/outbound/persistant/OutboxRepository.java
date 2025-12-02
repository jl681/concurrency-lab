package org.jl.orderprocessing.Infrastructure.outbound.persistant;


import org.jl.orderprocessing.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    // Used by the background scheduler to find events that need to be resent.
    // We sort by CreatedAt to ensure we send them in the correct order.
    List<OutboxEvent> findAllByOrderByCreatedAtAsc();

    // Optional: If you process them in batches, you might want a native query
    // to "lock" rows so multiple schedulers don't pick up the same event.
    // @Query(value = "SELECT * FROM outbox_events FOR UPDATE SKIP LOCKED LIMIT 10", nativeQuery = true)
    // List<OutboxEvent> findBatchForProcessing();
}
