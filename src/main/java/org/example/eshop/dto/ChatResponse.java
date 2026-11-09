package org.example.eshop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String reply;
    private String conversationId;
    private LocalDateTime timestamp;
    private boolean success;
    private String errorMessage;

    public static ChatResponse of(String reply, String conversationId) {
        return ChatResponse.builder()
                .reply(reply)
                .conversationId(conversationId)
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();
    }

    public static ChatResponse error(String errorMessage, String conversationId) {
        return ChatResponse.builder()
                .errorMessage(errorMessage)
                .conversationId(conversationId)
                .timestamp(LocalDateTime.now())
                .success(false)
                .build();
    }
}
