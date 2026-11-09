package org.example.eshop.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.example.eshop.entity.User;

import java.time.Instant;

@Entity
@Table(name = "tracking_event", indexes = {
        @Index(name = "idx_tracking_session_time", columnList = "session_id, occurred_at"),
        @Index(name = "idx_tracking_user_time", columnList = "user_id, occurred_at"),
        @Index(name = "idx_tracking_type_time", columnList = "event_type, occurred_at")
})
@Getter @Setter
public class TrackingEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private TrackingEventType eventType;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(length = 200)
    private String keyword;

    @Column(name = "result_count")
    private Long resultCount;

    private Integer quantity;

    @Column(name = "page_path", length = 200)
    private String pagePath;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
