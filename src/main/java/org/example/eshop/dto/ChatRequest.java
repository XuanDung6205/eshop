package org.example.eshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String message;

    /**
     * Mã phiên hội thoại (phục vụ quản lý ngữ cảnh đa lượt)
     */
    private String conversationId;

    /** Optional browser tracking session used to personalize product retrieval. */
    private String sessionId;

    /** Recent messages kept by the browser to support follow-up questions. */
    private List<ChatTurn> history;

    public record ChatTurn(String role, String content) {}

    /**
     * Giữ lại để tương thích với client cũ. Ngữ cảnh do client gửi không được
     * dùng làm nguồn dữ liệu; API chat tự truy vấn Vector Database cho mỗi câu hỏi.
     */
    @Deprecated
    private String context;
}
