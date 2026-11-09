package org.example.eshop.service.ai;

import org.example.eshop.dto.ProductReference;
import org.example.eshop.entity.Product;
import org.example.eshop.repository.ProductRepository;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service for the product RAG flow.
 *
 * <p>The vector store only identifies candidate product ids.  This service
 * deliberately loads those products again from MariaDB before building the
 * prompt, so price and stock are always taken from the current catalogue.</p>
 */
@Service
public class ProductAssistantService {

    private static final int DEFAULT_TOP_K = 5;
    private static final String NO_RESULTS_MESSAGE =
            "Mình chưa tìm thấy sản phẩm phù hợp với câu hỏi của bạn trong eShop. "
                    + "Bạn có thể cho mình biết thêm loại sản phẩm, nhu cầu hoặc khoảng giá không?";

    private final ProductVectorService productVectorService;
    private final ProductRepository productRepository;
    private final AiChatService aiChatService;

    public ProductAssistantService(ProductVectorService productVectorService,
                                   ProductRepository productRepository,
                                   AiChatService aiChatService) {
        this.productVectorService = productVectorService;
        this.productRepository = productRepository;
        this.aiChatService = aiChatService;
    }

    /**
     * Answers a product question with retrieved catalogue context.
     *
     * @param question the customer's natural-language question
     * @return the generated answer and the catalogue records used as sources
     */
    @Transactional(readOnly = true)
    public RagAnswer ask(String question) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được để trống");
        }

        List<Document> matches = productVectorService.search(question.trim(), DEFAULT_TOP_K);
        List<ProductReference> sources = loadCurrentSources(matches);
        if (sources.isEmpty()) {
            return new RagAnswer(NO_RESULTS_MESSAGE, List.of());
        }

        String context = buildContext(sources);
        String reply = aiChatService.chatWithContext(question.trim(), context);
        return new RagAnswer(reply, sources);
    }

    private List<ProductReference> loadCurrentSources(List<Document> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }

        Map<Long, Double> scoresByProductId = new LinkedHashMap<>();
        for (Document match : matches) {
            Long productId = productIdOf(match);
            if (productId != null) {
                scoresByProductId.putIfAbsent(productId, match.getScore());
            }
        }
        if (scoresByProductId.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> productsById = new LinkedHashMap<>();
        for (Product product : productRepository.findAllById(scoresByProductId.keySet())) {
            productsById.put(product.getId(), product);
        }

        List<ProductReference> sources = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : scoresByProductId.entrySet()) {
            Product product = productsById.get(entry.getKey());
            if (product != null) {
                sources.add(toReference(product, entry.getValue()));
            }
        }
        return List.copyOf(sources);
    }

    private ProductReference toReference(Product product, Double score) {
        return new ProductReference(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getPrice(),
                product.getQuantity(),
                "/products/" + product.getId(),
                score
        );
    }

    private String buildContext(List<ProductReference> sources) {
        StringBuilder context = new StringBuilder();
        for (ProductReference source : sources) {
            context.append("- Sản phẩm: ").append(source.name()).append('\n')
                    .append("  Danh mục: ").append(valueOrUnknown(source.categoryName())).append('\n')
                    .append("  Mô tả: ").append(valueOrUnknown(source.description())).append('\n')
                    .append("  Mã sản phẩm: ").append(source.id()).append('\n')
                    .append("  Giá hiện tại: ").append(formatPrice(source.price())).append(" VND\n")
                    .append("  Tồn kho hiện tại: ").append(source.quantity()).append(" sản phẩm\n")
                    .append("  Đường dẫn: ").append(source.url()).append('\n');
        }
        return context.toString().trim();
    }

    private String valueOrUnknown(String value) {
        return StringUtils.hasText(value) ? value : "chưa cập nhật";
    }

    private String formatPrice(BigDecimal price) {
        return price == null ? "chưa cập nhật" : price.toPlainString();
    }

    private Long productIdOf(Document document) {
        if (document == null || document.getMetadata() == null) {
            return null;
        }
        Object value = document.getMetadata().get("productId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.valueOf(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public record RagAnswer(String reply, List<ProductReference> sources) {
        public RagAnswer {
            sources = sources == null ? List.of() : List.copyOf(sources);
        }
    }
}
