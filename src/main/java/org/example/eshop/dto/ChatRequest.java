package org.example.eshop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /**
     * Dữ liệu ngữ cảnh bổ sung từ Vector Database (dành cho Thành viên 3 cắm RAG vào)
     */
    private String context;
}
