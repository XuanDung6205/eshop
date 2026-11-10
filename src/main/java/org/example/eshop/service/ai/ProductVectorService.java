package org.example.eshop.service.ai;

import org.example.eshop.entity.Product;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductVectorService {

    private final VectorStore vectorStore;

    public ProductVectorService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void indexProduct(Product product) {
        indexProducts(List.of(product));
    }

    public void indexProducts(List<Product> products) {
        if (products.isEmpty()) {
            return;
        }

        List<String> documentIds = new ArrayList<>(products.size() * 2);
        List<Document> documents = new ArrayList<>(products.size() * 2);
        for (Product product : products) {
            if (product.getId() == null) {
                throw new IllegalArgumentException("Sản phẩm phải được lưu trước khi lập chỉ mục");
            }

            String nameId = documentId(product.getId(), "NAME");
            String descriptionId = documentId(product.getId(), "DESCRIPTION");
            documentIds.add(nameId);
            documentIds.add(descriptionId);
            documents.add(new Document(nameId, product.getName(), metadata(product, "NAME")));
            if (StringUtils.hasText(product.getDescription())) {
                documents.add(new Document(descriptionId, product.getDescription(), metadata(product, "DESCRIPTION")));
            }
        }

        vectorStore.delete(documentIds);
        vectorStore.add(documents);
    }

    public void deleteProduct(Long productId) {
        vectorStore.delete(List.of(
                documentId(productId, "NAME"),
                documentId(productId, "DESCRIPTION")
        ));
    }

    public List<Document> search(String query, int topK) {
        return search(query, topK, 0.0);
    }

    public List<Document> search(String query, int topK, double similarityThreshold) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm không được để trống");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("Số kết quả phải lớn hơn 0");
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("Ngưỡng tương đồng phải nằm trong khoảng từ 0 đến 1");
        }
        return vectorStore.similaritySearch(
                SearchRequest.builder().query(query.trim()).topK(topK)
                        .similarityThreshold(similarityThreshold).build()
        );
    }

    private String documentId(Long productId, String sourceType) {
        String stableKey = "eshop:product:" + productId + ":" + sourceType;
        return UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Map<String, Object> metadata(Product product, String sourceType) {
        return Map.of(
                "productId", product.getId(),
                "sourceType", sourceType,
                "productName", product.getName()
        );
    }
}
