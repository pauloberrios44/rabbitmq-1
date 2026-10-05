package com.example.consumer;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    // PostgreSQL handles concurrent deliveries: the unique key serializes competing inserts.
    @Modifying
    @Query(value = "INSERT INTO processed_events (event_id, processed_at) VALUES (:id, CURRENT_TIMESTAMP) ON CONFLICT (event_id) DO NOTHING",
            nativeQuery = true)
    int claim(@Param("id") String eventId);
}
