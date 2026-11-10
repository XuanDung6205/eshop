package org.example.eshop.service;

import org.example.eshop.entity.Category;
import org.example.eshop.entity.Product;
import org.example.eshop.exception.NotFoundException;
import org.example.eshop.repository.CategoryRepository;
import org.example.eshop.repository.ProductRepository;
import org.example.eshop.service.ai.ProductVectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final int MAX_SHOP_VECTOR_SEARCH_DOCUMENTS = 100;
    private static final double MIN_SHOP_SIMILARITY = 0.55;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVectorService productVectorService;

    public Page<Product> searchForShop(String keyword, Long categoryId, int page, int size) {
        String query = StringUtils.hasText(keyword) ? keyword.trim() : null;
        int normalizedPage = Math.max(page, 0);
        if (query == null) {
            return search(null, categoryId, normalizedPage, size);
        }

        long requestedDocuments = ((long) normalizedPage + 1) * size * 2;
        int topK = categoryId == null
                ? (int) Math.min(MAX_SHOP_VECTOR_SEARCH_DOCUMENTS, Math.max(20L, requestedDocuments))
                : MAX_SHOP_VECTOR_SEARCH_DOCUMENTS;

        List<Long> mentionedCategories = categoryId == null
                ? productRepository.findCategoryIdsMentionedInQuery(query.toLowerCase(Locale.ROOT))
                : List.of();
        Set<Long> effectiveCategoryIds = categoryId != null
                ? Set.of(categoryId)
                : (mentionedCategories != null && !mentionedCategories.isEmpty()
                        ? Set.copyOf(mentionedCategories)
                        : Set.of());

        List<Product> matches = semanticSearch(query, topK).stream()
                .filter(result -> result.score() == null || result.score() >= MIN_SHOP_SIMILARITY)
                .map(SemanticSearchResult::product)
                .filter(product -> effectiveCategoryIds.isEmpty()
                        || (product.getCategory() != null
                                && effectiveCategoryIds.contains(product.getCategory().getId())))
                .toList();

        int fromIndex = (int) Math.min(matches.size(), (long) normalizedPage * size);
        int toIndex = Math.min(fromIndex + size, matches.size());
        return new PageImpl<>(matches.subList(fromIndex, toIndex),
                PageRequest.of(normalizedPage, size), matches.size());
    }

    public Page<Product> search(String keyword, Long categoryId, int page, int size) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return productRepository.search(kw, categoryId,
                PageRequest.of(Math.max(page, 0), size, Sort.by("id").descending()));
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm id = " + id));
    }

    public List<SemanticSearchResult> semanticSearch(String query, int topK) {
        List<Document> matches = productVectorService.search(query, topK);
        Map<Long, Product> productsById = new LinkedHashMap<>();
        for (Product product : productRepository.findAllById(matches.stream()
                .map(this::productIdFrom)
                .filter(id -> id != null)
                .distinct()
                .toList())) {
            productsById.put(product.getId(), product);
        }

        Map<Long, SemanticSearchResult> resultsByProductId = new LinkedHashMap<>();
        for (Document match : matches) {
            Long productId = productIdFrom(match);
            Product product = productId == null ? null : productsById.get(productId);
            if (product != null) {
                resultsByProductId.putIfAbsent(productId, new SemanticSearchResult(
                        product,
                        match.getScore(),
                        String.valueOf(match.getMetadata().getOrDefault("sourceType", ""))));
            }
        }
        return List.copyOf(resultsByProductId.values());
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public Product save(Product product, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục id = " + categoryId));
        product.setCategory(category);
        Product savedProduct = productRepository.save(product);
        productVectorService.indexProduct(savedProduct);
        return savedProduct;
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public void delete(Long id) {
        try {
            Product product = findById(id);
            productRepository.delete(product);
            productRepository.flush();
            productVectorService.deleteProduct(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Sản phẩm đã có trong đơn hàng, không thể xoá");
        }
    }

    @PreAuthorize("hasRole('SELLER')")
    @Transactional
    public int reindexAllProducts() {
        List<Product> products = productRepository.findAll();
        productVectorService.indexProducts(products);
        return products.size();
    }

    private Long productIdFrom(Document document) {
        Object value = document.getMetadata().get("productId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public record SemanticSearchResult(Product product, Double score, String matchedField) {
    }
}
