package org.example.eshop.service.ai;

import org.example.eshop.entity.Product;
import org.example.eshop.entity.Category;
import org.example.eshop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import org.mockito.ArgumentCaptor;

class ProductAssistantServiceTest {

    private ProductVectorService vectorService;
    private ProductRepository productRepository;
    private AiChatService aiChatService;
    private ProductAssistantService assistantService;

    @BeforeEach
    void setUp() {
        vectorService = mock(ProductVectorService.class);
        productRepository = mock(ProductRepository.class);
        aiChatService = mock(AiChatService.class);
        assistantService = new ProductAssistantService(vectorService, productRepository, aiChatService);
    }

    @Test
    void retrievesCurrentProductDataBeforeCallingModel() {
        Product product = new Product();
        product.setId(15L);
        product.setName("iPhone 15");
        product.setDescription("Camera tốt, pin dùng cả ngày");
        Category category = new Category();
        category.setName("Điện thoại");
        product.setCategory(category);
        product.setPrice(new BigDecimal("19990000"));
        product.setQuantity(7);

        when(vectorService.search("điện thoại pin tốt", 5)).thenReturn(List.of(
                new Document("vector-15", "iPhone 15", Map.of("productId", 15L))));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(aiChatService.chatWithContext(any(), any())).thenReturn("Có iPhone 15 phù hợp.");

        ProductAssistantService.RagAnswer answer = assistantService.ask("điện thoại pin tốt");

        assertEquals("Có iPhone 15 phù hợp.", answer.reply());
        assertEquals(1, answer.sources().size());
        assertEquals("/products/15", answer.sources().get(0).url());
        assertEquals(new BigDecimal("19990000"), answer.sources().get(0).price());
        assertEquals(7, answer.sources().get(0).quantity());

        ArgumentCaptor<String> context = ArgumentCaptor.forClass(String.class);
        verify(aiChatService).chatWithContext(eq("điện thoại pin tốt"), context.capture());
        assertTrue(context.getValue().contains("Giá hiện tại: 19990000 VND"));
        assertTrue(context.getValue().contains("Tồn kho hiện tại: 7 sản phẩm"));
        assertTrue(context.getValue().contains("Mô tả: Camera tốt, pin dùng cả ngày"));
        assertTrue(context.getValue().contains("Đường dẫn: /products/15"));
    }

    @Test
    void returnsFollowUpWhenVectorSearchHasNoCurrentProducts() {
        when(vectorService.search("sản phẩm không có", 5)).thenReturn(List.of());

        ProductAssistantService.RagAnswer answer = assistantService.ask("sản phẩm không có");

        assertTrue(answer.reply().contains("chưa tìm thấy"));
        assertTrue(answer.sources().isEmpty());
        verify(productRepository, never()).findAllById(any());
        verify(aiChatService, never()).chatWithContext(any(), any());
    }

    @Test
    void rejectsBlankQuestion() {
        assertThrows(IllegalArgumentException.class, () -> assistantService.ask("  "));
        verify(vectorService, never()).search(any(), any(Integer.class));
    }
}
