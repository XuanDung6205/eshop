package org.example.eshop.service.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_EMBEDDING_LIVE_TEST", matches = "true")
class SpringAiVectorLiveTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private VectorStore vectorStore;

    @Test
    void embedsStoresAndSearchesDocument() {
        String text = "Điện thoại chụp ảnh đẹp và pin lâu";
        float[] embedding = embeddingModel.embed(text);
        assertEquals(768, embedding.length);
        System.out.printf("Spring AI embedding OK: dimensions=%d, first values=%s%n",
                embedding.length, List.of(embedding[0], embedding[1], embedding[2], embedding[3],
                        embedding[4], embedding[5], embedding[6], embedding[7]));

        String documentId = "live-test-" + UUID.randomUUID();
        vectorStore.add(List.of(new Document(
                documentId,
                text,
                Map.of("productId", -1, "sourceType", "TEST")
        )));
        try {
            List<Document> results = vectorStore.similaritySearch(text);
            assertTrue(results.stream().anyMatch(document -> documentId.equals(document.getId())));
            System.out.printf("Spring AI MariaDB vector search OK: document=%s, results=%d%n",
                    documentId, results.size());
        } finally {
            vectorStore.delete(List.of(documentId));
        }
    }
}
