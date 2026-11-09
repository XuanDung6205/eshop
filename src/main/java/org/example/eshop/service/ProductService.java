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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVectorService productVectorService;

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
                        String.valueOf(match.getMetadata().getOrDefault("sourceType", ""))
                ));
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
