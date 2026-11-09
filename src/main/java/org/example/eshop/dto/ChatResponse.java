package org.example.eshop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    /** Products retrieved from the vector store and used to ground the answer. */
    private List<ProductReference> sources;

    public static ChatResponse of(String reply, String conversationId) {
        return of(reply, conversationId, List.of());
    }

    public static ChatResponse of(String reply, String conversationId, List<ProductReference> sources) {
        return ChatResponse.builder()
                .reply(reply)
                .conversationId(conversationId)
                .timestamp(LocalDateTime.now())
                .success(true)
                .sources(sources == null ? List.of() : List.copyOf(sources))
                .build();
    }

    public static ChatResponse error(String errorMessage, String conversationId) {
        return ChatResponse.builder()
                .errorMessage(errorMessage)
                .conversationId(conversationId)
                .timestamp(LocalDateTime.now())
                .success(false)
                .sources(List.of())
                .build();
    }
}
