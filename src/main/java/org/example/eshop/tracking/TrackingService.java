package org.example.eshop.tracking;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.eshop.entity.Order;
import org.example.eshop.entity.User;
import org.example.eshop.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TrackingService {
    private static final Logger log = LoggerFactory.getLogger(TrackingService.class);
    private static final Pattern SESSION_ID = Pattern.compile("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");

    private final TrackingEventRepository eventRepository;
    private final UserRepository userRepository;

    public void recordBrowserEvent(TrackingEventRequest request, Principal principal) {
        if (!isBrowserEvent(request.eventType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event type is server-owned");
        }
        validateDetails(request);

        TrackingEvent event = new TrackingEvent();
        event.setEventType(request.eventType());
        event.setSessionId(request.sessionId());
        event.setOccurredAt(request.occurredAt());
        event.setReceivedAt(Instant.now());
        event.setProductId(request.productId());
        event.setCategoryId(request.categoryId());
        event.setKeyword(request.keyword() == null ? null : request.keyword().trim());
        event.setResultCount(request.resultCount());
        event.setQuantity(request.quantity());
        event.setPagePath(request.pagePath());
        event.setUser(findUser(principal));
        eventRepository.save(event);
    }

    public void recordCartEvent(TrackingEventType type, Long productId, Integer quantity,
                                String clientSessionId, HttpServletRequest request, Principal principal) {
        try {
            TrackingEvent event = serverEvent(type, clientSessionId, request);
            event.setProductId(productId);
            event.setQuantity(quantity);
            event.setUser(findUser(principal));
            eventRepository.save(event);
        } catch (RuntimeException ex) {
            log.warn("Could not record cart tracking event {}", type, ex);
        }
    }

    public void recordOrderPlaced(Order order, String clientSessionId, HttpServletRequest request) {
        try {
            TrackingEvent event = serverEvent(TrackingEventType.ORDER_PLACED, clientSessionId, request);
            event.setOrderId(order.getId());
            event.setUser(order.getUser());
            eventRepository.save(event);
        } catch (RuntimeException ex) {
            log.warn("Could not record placed order tracking event", ex);
        }
    }

    private TrackingEvent serverEvent(TrackingEventType type, String clientSessionId, HttpServletRequest request) {
        TrackingEvent event = new TrackingEvent();
        event.setEventType(type);
        event.setSessionId(clientSessionId != null && SESSION_ID.matcher(clientSessionId).matches()
                ? clientSessionId : request.getSession().getId());
        Instant now = Instant.now();
        event.setOccurredAt(now);
        event.setReceivedAt(now);
        return event;
    }

    private User findUser(Principal principal) {
        return principal == null ? null : userRepository.findByUsername(principal.getName()).orElse(null);
    }

    private boolean isBrowserEvent(TrackingEventType type) {
        return switch (type) {
            case PAGE_VIEW, PRODUCT_VIEW, SEARCH, FILTER, ADD_TO_CART_CLICK,
                    CART_UPDATE_CLICK, CART_REMOVE_CLICK, CART_CLEAR_CLICK, CHECKOUT_START -> true;
            default -> false;
        };
    }

    private void validateDetails(TrackingEventRequest request) {
        TrackingEventType type = request.eventType();
        if ((type == TrackingEventType.PRODUCT_VIEW || type == TrackingEventType.ADD_TO_CART_CLICK
                || type == TrackingEventType.CART_UPDATE_CLICK || type == TrackingEventType.CART_REMOVE_CLICK)
                && request.productId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        if (type == TrackingEventType.SEARCH && (request.keyword() == null || request.keyword().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "keyword is required");
        }
        if (type == TrackingEventType.FILTER && request.categoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId is required");
        }
        if (type == TrackingEventType.PAGE_VIEW && request.pagePath() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pagePath is required");
        }
    }
}
