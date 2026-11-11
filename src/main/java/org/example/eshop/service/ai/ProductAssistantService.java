package org.example.eshop.service.ai;

import org.example.eshop.dto.ProductReference;
import org.example.eshop.entity.Product;
import org.example.eshop.repository.ProductRepository;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Application service for the product RAG flow.
 *
 * <p>The vector store only identifies candidate product ids.  This service
 * deliberately loads those products again from MariaDB before building the
 * prompt, so price and stock are always taken from the current catalogue.</p>
 */
@Service
public class ProductAssistantService {

    private static final Logger log = LoggerFactory.getLogger(ProductAssistantService.class);
    private static final int VECTOR_SEARCH_LIMIT = 5;
    private static final int SOURCE_LIMIT = 5;
    private static final double MIN_SIMILARITY = 0.55;
    private static final String NO_RESULTS_MESSAGE =
            "Mình chưa tìm thấy sản phẩm trang trí nội thất phù hợp với câu hỏi của bạn trong DecorHome. "
                    + "Bạn có thể cho mình biết thêm loại sản phẩm, phong cách (Bắc Âu, tối giản...), không gian bài trí hoặc khoảng giá không?";

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
        return ask(question, question);
    }

    @Transactional(readOnly = true)
    public RagAnswer ask(String question, String retrievalQuery) {
        return ask(question, retrievalQuery, List.of());
    }

    @Transactional(readOnly = true)
    public RagAnswer ask(String question, String retrievalQuery, List<org.example.eshop.dto.ChatRequest.ChatTurn> history) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được để trống");
        }

        String query = StringUtils.hasText(retrievalQuery) ? retrievalQuery : question;
        String historyText = formatHistory(history);
        Set<Long> currentQuestionCategories = findMentionedCategories(question);
        Set<Long> historyCategories = findMentionedCategories(historyText);
        // A clear category in the latest question must override older conversation topics.
        Set<Long> requestedCategories = currentQuestionCategories.isEmpty()
                ? historyCategories
                : currentQuestionCategories;
        List<Document> matches;
        try {
            matches = productVectorService.search(query.trim(), VECTOR_SEARCH_LIMIT);
        } catch (RuntimeException exception) {
            log.warn("Vector retrieval unavailable for chat; using category fallback when possible ({})",
                    exception.getClass().getSimpleName());
            matches = List.of();
        }
        List<ProductReference> sources = loadCurrentSources(matches, requestedCategories);
        if (!requestedCategories.isEmpty() && sources.size() < SOURCE_LIMIT) {
            sources = appendCategoryProducts(sources, requestedCategories);
        }
        if (sources.isEmpty()) {
            return new RagAnswer(NO_RESULTS_MESSAGE, List.of());
        }

        String context = buildContext(sources);
        String reply = aiChatService.chatWithContext(question.trim(), context, historyText);
        return new RagAnswer(reply, sources);
    }

    private Set<Long> findMentionedCategories(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        return Set.copyOf(productRepository.findCategoryIdsMentionedInQuery(text.toLowerCase(Locale.ROOT)));
    }

    private String formatHistory(List<org.example.eshop.dto.ChatRequest.ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder transcript = new StringBuilder();
        history.stream()
                .skip(Math.max(0, history.size() - 8L))
                .filter(turn -> turn != null && StringUtils.hasText(turn.role())
                        && StringUtils.hasText(turn.content()))
                .filter(turn -> "user".equals(turn.role()) || "assistant".equals(turn.role()))
                .forEach(turn -> {
                    String content = turn.content().trim();
                    if (content.length() > 800) {
                        content = content.substring(0, 800);
                    }
                    if (!transcript.isEmpty()) {
                        transcript.append('\n');
                    }
                    transcript.append("user".equals(turn.role()) ? "Khách: " : "Trợ lý: ")
                            .append(content);
                });
        return transcript.toString();
    }

    private List<ProductReference> loadCurrentSources(List<Document> matches, Set<Long> requestedCategories) {
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
            if (product != null && product.getQuantity() != null && product.getQuantity() > 0
                    && (requestedCategories.isEmpty() || (product.getCategory() != null
                    && requestedCategories.contains(product.getCategory().getId())))
                    && (entry.getValue() == null || entry.getValue() >= MIN_SIMILARITY
                    || !requestedCategories.isEmpty())) {
                sources.add(toReference(product, entry.getValue()));
            }
            if (sources.size() == SOURCE_LIMIT) {
                break;
            }
        }
        return List.copyOf(sources);
    }

    private List<ProductReference> appendCategoryProducts(List<ProductReference> initial,
                                                           Set<Long> requestedCategories) {
        List<ProductReference> sources = new ArrayList<>(initial);
        Set<Long> existingIds = new java.util.HashSet<>();
        sources.stream().map(ProductReference::id).filter(Objects::nonNull).forEach(existingIds::add);
        for (Long categoryId : requestedCategories) {
            for (Product product : productRepository.findInStockByCategoryId(
                    categoryId, PageRequest.of(0, SOURCE_LIMIT))) {
                if (existingIds.add(product.getId())) {
                    sources.add(toReference(product, null));
                    if (sources.size() == SOURCE_LIMIT) {
                        return List.copyOf(sources);
                    }
                }
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
