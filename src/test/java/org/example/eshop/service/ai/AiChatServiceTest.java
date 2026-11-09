package org.example.eshop.service.ai;

import org.example.eshop.config.AiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class AiChatServiceTest {

    private AiChatService aiChatService;
    private AiConfig aiConfig;

    @BeforeEach
    void setUp() {
        aiConfig = new AiConfig();
        RestClient restClient = aiConfig.geminiRestClient();
        aiChatService = new AiChatService(restClient, aiConfig);
    }

    @Test
    @DisplayName("Khi chưa cấu hình Gemini API Key, trả về thông báo hướng dẫn demo thân thiện")
    void testChatWithoutApiKeyReturnsFriendlyDemoMessage() {
        String reply = aiChatService.chat("Chào shop, bạn có bán laptop không?");
        assertNotNull(reply);
        assertTrue(reply.contains("Google Gemini API Key") || reply.contains("Demo"));
    }

    @Test
    @DisplayName("Hàm chatWithContext xử lý bình thường ngay cả khi context rỗng")
    void testChatWithContextEmptyFallsBackToRegularChat() {
        String reply = aiChatService.chatWithContext("iPhone 15 giá bao nhiêu?", "");
        assertNotNull(reply);
    }
}

