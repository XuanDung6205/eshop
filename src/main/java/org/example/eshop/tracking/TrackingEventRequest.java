package org.example.eshop.tracking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record TrackingEventRequest(
        @NotNull TrackingEventType eventType,
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}") String sessionId,
        @NotNull Instant occurredAt,
        @Positive Long productId,
        @Positive Long categoryId,
        @Size(max = 200) String keyword,
        @PositiveOrZero Long resultCount,
        @PositiveOrZero Integer quantity,
        @Size(max = 200) @Pattern(regexp = "/[^?#]*") String pagePath
) {
}
