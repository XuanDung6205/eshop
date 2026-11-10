package org.example.eshop.tracking;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    @Query("SELECT e FROM TrackingEvent e WHERE e.user.id = :userId AND e.occurredAt >= :since " +
            "AND e.eventType IN :eventTypes ORDER BY e.occurredAt DESC")
    List<TrackingEvent> findRecentForUser(@Param("userId") Long userId,
                                          @Param("since") Instant since,
                                          @Param("eventTypes") List<TrackingEventType> eventTypes,
                                          Pageable pageable);

    @Query("SELECT e FROM TrackingEvent e WHERE e.sessionId = :sessionId AND e.occurredAt >= :since " +
            "AND e.eventType IN :eventTypes ORDER BY e.occurredAt DESC")
    List<TrackingEvent> findRecentForSession(@Param("sessionId") String sessionId,
                                             @Param("since") Instant since,
                                             @Param("eventTypes") List<TrackingEventType> eventTypes,
                                             Pageable pageable);
}
