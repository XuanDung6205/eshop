package org.example.eshop.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshop.dto.ChatRequest;
import org.example.eshop.dto.ChatResponse;
import org.example.eshop.service.ai.ProductAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/chat", "/api/chat/"})
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final ProductAssistantService productAssistantService;

    /**
     * Endpoint chính tiếp nhận câu hỏi của người dùng và trả về phản hồi từ Google Gemini AI.
     * Tự động truy vấn Vector Database và gửi dữ liệu sản phẩm hiện hành vào prompt RAG.
     * Hỗ trợ cả /api/chat và /api/chat/
     *
     * POST /api/chat
     * Body: { "message": "Shop có bán iPhone 15 không?", "conversationId": "..." }
     */
    @PostMapping({"", "/"})
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        log.info("Nhận tin nhắn chat [conversationId: {}]: {}", conversationId, request.getMessage());

        ProductAssistantService.RagAnswer answer = productAssistantService.ask(request.getMessage());

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
}
