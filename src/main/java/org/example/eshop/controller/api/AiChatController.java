package org.example.eshop.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshop.dto.ChatRequest;
import org.example.eshop.dto.ChatResponse;
import org.example.eshop.service.ProductRecommendationService;
import org.example.eshop.service.ai.ProductAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.List;

@RestController
@RequestMapping({"/api/chat", "/api/chat/"})
@RequiredArgsConstructor
@Slf4j
public class AiChatController {
    private static final Pattern SESSION_ID = Pattern.compile(
            "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");

    private final ProductAssistantService productAssistantService;
    private final ProductRecommendationService productRecommendationService;

    /**
     * Endpoint chính tiếp nhận câu hỏi của người dùng và trả về phản hồi từ Google Gemini AI.
     * Tự động truy vấn Vector Database và gửi dữ liệu sản phẩm hiện hành vào prompt RAG.
     * Hỗ trợ cả /api/chat và /api/chat/
     *
     * POST /api/chat
     * Body: { "message": "Shop có bán iPhone 15 không?", "conversationId": "..." }
     */
    @PostMapping({"", "/"})
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, Principal principal) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        log.info("Nhận tin nhắn chat [conversationId: {}]: {}", conversationId, request.getMessage());

        String sessionId = request.getSessionId();
        if (sessionId != null && !SESSION_ID.matcher(sessionId).matches()) {
            sessionId = null;
        }
        String username = principal == null ? null : principal.getName();
        String retrievalQuery = withConversationContext(
                productRecommendationService.buildPersonalizedSearchQuery(request.getMessage(), username, sessionId),
                request.getHistory());
        ProductAssistantService.RagAnswer answer = productAssistantService.ask(
                request.getMessage(), retrievalQuery, request.getHistory());

        return ResponseEntity.ok(ChatResponse.of(answer.reply(), conversationId, answer.sources()));
    }

    /**
     * Endpoint kiểm tra trạng thái hoạt động của lớp AI (Health Check)
     * GET /api/chat/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Spring AI - Google Gemini Assistant",
                "provider", "Google Gemini",
                "ragReady", true
        ));
    }

    private String withConversationContext(String query, List<ChatRequest.ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return query;
        }
        StringBuilder enriched = new StringBuilder(query);
        history.stream()
                .skip(Math.max(0, history.size() - 4L))
                .filter(turn -> turn != null && turn.content() != null
                        && ("user".equals(turn.role()) || "assistant".equals(turn.role())))
                .forEach(turn -> {
                    String content = turn.content().trim();
                    if (content.length() > 300) {
                        content = content.substring(0, 300);
                    }
                    if (!content.isEmpty()) {
                        enriched.append(' ').append(content);
                    }
                });
        return enriched.substring(0, Math.min(enriched.length(), 1200));
    }
}
