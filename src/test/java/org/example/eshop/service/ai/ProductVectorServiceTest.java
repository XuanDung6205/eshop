package org.example.eshop.service.ai;

import org.example.eshop.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductVectorServiceTest {

    private VectorStore vectorStore;
    private ProductVectorService productVectorService;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        productVectorService = new ProductVectorService(vectorStore);
    }

    @Test
    void indexesNameAndDescriptionAsSeparateDocuments() {
        Product product = new Product();
        product.setId(42L);
        product.setName("Điện thoại A");
        product.setDescription("Chụp ảnh đẹp");

        productVectorService.indexProduct(product);

        String nameId = documentId(42L, "NAME");
        String descriptionId = documentId(42L, "DESCRIPTION");
        verify(vectorStore).delete(List.of(nameId, descriptionId));
        verify(vectorStore).add(argThat(documents ->
                documents.size() == 2
                        && documents.stream().anyMatch(document ->
                        nameId.equals(document.getId())
                                && "NAME".equals(document.getMetadata().get("sourceType")))
                        && documents.stream().anyMatch(document ->
                        descriptionId.equals(document.getId())
                                && "DESCRIPTION".equals(document.getMetadata().get("sourceType")))));
    }

    @Test
    void omitsBlankDescriptionWhenIndexing() {
        Product product = new Product();
        product.setId(42L);
        product.setName("Điện thoại A");
        product.setDescription("  ");

        productVectorService.indexProduct(product);

        verify(vectorStore).add(argThat(documents ->
                documents.size() == 1 && documentId(42L, "NAME").equals(documents.get(0).getId())));
    }

    @Test
    void rejectsBlankSearchQuery() {
        assertThrows(IllegalArgumentException.class, () -> productVectorService.search(" ", 5));
    }

    @Test
    void searchesWithTopKAndTrimmedQuery() {
        Document result = new Document(documentId(42L, "NAME"), "Điện thoại A", Map.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(result));

        assertEquals(List.of(result), productVectorService.search("  điện thoại  ", 3));
        verify(vectorStore).similaritySearch(argThat((SearchRequest request) ->
                "điện thoại".equals(request.getQuery()) && request.getTopK() == 3));
    }

    private String documentId(Long productId, String sourceType) {
        String stableKey = "eshop:product:" + productId + ":" + sourceType;
        return UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
