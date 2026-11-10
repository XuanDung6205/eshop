package org.example.eshop.service;

import lombok.RequiredArgsConstructor;
import org.example.eshop.entity.Category;
import org.example.eshop.entity.OrderDetail;
import org.example.eshop.entity.Product;
import org.example.eshop.entity.User;
import org.example.eshop.repository.CategoryRepository;
import org.example.eshop.repository.OrderDetailRepository;
import org.example.eshop.repository.ProductRepository;
import org.example.eshop.repository.UserRepository;
import org.example.eshop.service.ai.ProductVectorService;
import org.example.eshop.tracking.TrackingEventRepository;
import org.example.eshop.tracking.TrackingEvent;
import org.example.eshop.tracking.TrackingEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductRecommendationService {
    private static final Logger log = LoggerFactory.getLogger(ProductRecommendationService.class);
    private static final int VECTOR_SEARCH_LIMIT = 20;
    private static final int CATEGORY_FALLBACK_LIMIT = 8;
    private static final double MIN_RELATED_SIMILARITY = 0.58;
    private static final int HISTORY_LIMIT = 100;
    private static final int PURCHASE_WEIGHT = 7;
    private static final List<TrackingEventType> RECOMMENDATION_EVENTS = List.of(
            TrackingEventType.PRODUCT_VIEW,
            TrackingEventType.ADD_TO_CART_CLICK,
            TrackingEventType.ADD_TO_CART,
            TrackingEventType.CART_UPDATE,
            TrackingEventType.SEARCH,
            TrackingEventType.FILTER,
            TrackingEventType.ORDER_PLACED);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final ProductVectorService productVectorService;

    /** Builds a vector-search query enriched with recent behavior for the chat RAG flow. */
    @Transactional(readOnly = true)
    public String buildPersonalizedSearchQuery(String question, String username, String sessionId) {
        User user = StringUtils.hasText(username)
                ? userRepository.findByUsername(username).orElse(null)
                : null;
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        Map<Long, TrackingEvent> recentEvents = new HashMap<>();

        if (user != null) {
            trackingEventRepository.findRecentForUser(user.getId(), since, RECOMMENDATION_EVENTS,
                            PageRequest.of(0, HISTORY_LIMIT))
                    .forEach(event -> recentEvents.put(event.getId(), event));
        }
        if (StringUtils.hasText(sessionId)) {
            trackingEventRepository.findRecentForSession(sessionId, since, RECOMMENDATION_EVENTS,
                            PageRequest.of(0, HISTORY_LIMIT))
                    .forEach(event -> recentEvents.put(event.getId(), event));
        }

        List<TrackingEvent> events = recentEvents.values().stream()
                .sorted(Comparator.comparing(TrackingEvent::getOccurredAt).reversed())
                .limit(HISTORY_LIMIT)
                .toList();
        List<String> interests = new ArrayList<>();
        interests.add(question.trim());
        events.stream()
                .filter(event -> event.getEventType() == TrackingEventType.SEARCH)
                .map(TrackingEvent::getKeyword)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(3)
                .forEach(interests::add);

        Map<Long, Integer> productScores = new HashMap<>();
        Set<Long> orderIds = new HashSet<>();
        for (TrackingEvent event : events) {
            if (event.getProductId() != null) {
                productScores.merge(event.getProductId(), weight(event.getEventType()), Integer::sum);
            }
            if (event.getEventType() == TrackingEventType.ORDER_PLACED && event.getOrderId() != null) {
                orderIds.add(event.getOrderId());
            }
        }
        if (!orderIds.isEmpty()) {
            orderDetailRepository.findByOrderIdIn(orderIds).stream()
                    .map(OrderDetail::getProduct)
                    .filter(Objects::nonNull)
                    .forEach(product -> productScores.merge(product.getId(), PURCHASE_WEIGHT, Integer::sum));
        }

        Set<Long> profileProductIds = new LinkedHashSet<>();
        productScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .forEach(profileProductIds::add);
        if (!profileProductIds.isEmpty()) {
            productRepository.findAllById(profileProductIds).stream()
                    .map(this::productText)
                    .forEach(interests::add);
        }

        String query = String.join(" ", interests);
        return query.substring(0, Math.min(query.length(), 1200));
    }

    @Transactional(readOnly = true)
    public List<Product> findFor(Product product, String username, String sessionId) {
        User user = StringUtils.hasText(username)
                ? userRepository.findByUsername(username).orElse(null)
                : null;
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        Map<Long, TrackingEvent> recentEvents = new HashMap<>();

        if (user != null) {
            trackingEventRepository.findRecentForUser(user.getId(), since, RECOMMENDATION_EVENTS,
                            PageRequest.of(0, HISTORY_LIMIT))
                    .forEach(event -> recentEvents.put(event.getId(), event));
        }
        if (StringUtils.hasText(sessionId)) {
            trackingEventRepository.findRecentForSession(sessionId, since, RECOMMENDATION_EVENTS,
                            PageRequest.of(0, HISTORY_LIMIT))
                    .forEach(event -> recentEvents.put(event.getId(), event));
        }

        List<TrackingEvent> events = recentEvents.values().stream()
                .sorted(Comparator.comparing(TrackingEvent::getOccurredAt).reversed())
                .limit(HISTORY_LIMIT)
                .toList();
        Map<Long, Integer> productInterestScores = new HashMap<>();
        Set<Long> orderIds = new HashSet<>();

        for (TrackingEvent event : events) {
            if (event.getProductId() != null) {
                productInterestScores.merge(event.getProductId(), weight(event.getEventType()), Integer::sum);
            }
            if (event.getEventType() == TrackingEventType.ORDER_PLACED && event.getOrderId() != null) {
                orderIds.add(event.getOrderId());
            }
        }

        if (!orderIds.isEmpty()) {
            List<OrderDetail> purchasedDetails = orderDetailRepository.findByOrderIdIn(orderIds);
            for (OrderDetail detail : purchasedDetails) {
                Product purchased = detail.getProduct();
                if (purchased != null) {
                    productInterestScores.merge(purchased.getId(), PURCHASE_WEIGHT, Integer::sum);
                }
            }
        }

        // Only load the top-scored products (up to 3) instead of loading every interacted product
        List<Long> topScoredProductIds = productInterestScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(3)
                .toList();

        List<Product> topInteractedProducts = topScoredProductIds.isEmpty()
                ? Collections.emptyList()
                : productRepository.findAllById(topScoredProductIds);

        // Include recent user search keywords (up to 2) to further align recommendations with intent
        List<String> recentSearchKeywords = events.stream()
                .filter(event -> event.getEventType() == TrackingEventType.SEARCH)
                .map(TrackingEvent::getKeyword)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(2)
                .toList();

        // Only exclude the current product itself
        Set<Long> excludedProductIds = new HashSet<>();
        excludedProductIds.add(product.getId());

        String interestQuery = buildInterestQuery(topInteractedProducts, recentSearchKeywords, product);

        List<Product> recommendations = new ArrayList<>();
        Set<Long> collectedIds = new HashSet<>(excludedProductIds);

        // 1. TẦNG 1 (Ưu tiên cao nhất): TẤT CẢ sản phẩm cùng danh mục trong kho (substitutes)
        // Ví dụ: Xem Chuột -> hiển thị toàn bộ các mẫu Chuột còn lại của shop trước tiên
        if (product.getCategory() != null) {
            List<Product> sameCategory = productRepository.findRelatedProducts(
                    product.getCategory().getId(), product.getId(), PageRequest.of(0, 10));
            for (Product p : sameCategory) {
                if (isInStock(p) && collectedIds.add(p.getId())) {
                    recommendations.add(p);
                }
            }
        }

        // 2. TẦNG 2: Gợi ý theo AI Vector Search & Lịch sử hành vi người dùng (personalized)
        if (recommendations.size() < 8) {
            List<Product> vectorRecs = findVectorRecommendations(interestQuery, excludedProductIds, product);
            for (Product p : vectorRecs) {
                if (collectedIds.add(p.getId())) {
                    recommendations.add(p);
                    if (recommendations.size() >= 8) {
                        break;
                    }
                }
            }
        }

        // 3. TẦNG 3: Sản phẩm bổ trợ / Mua cùng theo hệ sinh thái (Complementary / Cross-category)
        // Ví dụ: Bàn phím -> gợi ý Chuột, Màn hình, Laptop
        if (recommendations.size() < 8 && product.getCategory() != null) {
            int needed = 8 - recommendations.size();
            List<Product> complementary = findComplementaryProducts(product, collectedIds, needed);
            for (Product p : complementary) {
                if (isInStock(p) && collectedIds.add(p.getId())) {
                    recommendations.add(p);
                }
            }
        }

        return recommendations;
    }

    private static final Map<String, List<String>> COMPLEMENTARY_CATEGORIES = Map.ofEntries(
            Map.entry("bàn phím", List.of("chuột", "màn hình", "laptop", "tai nghe")),
            Map.entry("chuột", List.of("bàn phím", "màn hình", "laptop", "tai nghe")),
            Map.entry("màn hình", List.of("laptop", "chuột", "bàn phím", "loa")),
            Map.entry("laptop", List.of("chuột", "bàn phím", "màn hình", "tai nghe", "lưu trữ")),
            Map.entry("máy tính bảng", List.of("bàn phím", "tai nghe", "sạc", "điện thoại")),
            Map.entry("điện thoại", List.of("tai nghe", "sạc", "đồng hồ", "máy tính bảng")),
            Map.entry("đồng hồ", List.of("điện thoại", "tai nghe", "sạc")),
            Map.entry("tai nghe", List.of("loa", "điện thoại", "laptop", "đồng hồ")),
            Map.entry("loa", List.of("tai nghe", "điện thoại", "laptop")),
            Map.entry("máy ảnh", List.of("lưu trữ", "sạc")),
            Map.entry("thiết bị lưu trữ", List.of("laptop", "máy in", "thiết bị mạng")),
            Map.entry("thiết bị mạng", List.of("laptop", "lưu trữ", "máy in")),
            Map.entry("máy in", List.of("laptop", "lưu trữ")),
            Map.entry("phụ kiện sạc", List.of("điện thoại", "tai nghe", "đồng hồ", "laptop")),
            Map.entry("gia dụng", List.of("gia dụng"))
    );

    private List<Product> findComplementaryProducts(Product currentProduct, Set<Long> excludedIds, int limit) {
        if (currentProduct.getCategory() == null || limit <= 0) {
            return Collections.emptyList();
        }
        String currentCatName = currentProduct.getCategory().getName().toLowerCase();

        List<String> targetCategoryKeywords = Collections.emptyList();
        for (Map.Entry<String, List<String>> entry : COMPLEMENTARY_CATEGORIES.entrySet()) {
            if (currentCatName.contains(entry.getKey())) {
                targetCategoryKeywords = entry.getValue();
                break;
            }
        }

        if (targetCategoryKeywords.isEmpty()) {
            return Collections.emptyList();
        }

        List<Category> allCategories = categoryRepository.findAll();
        List<Product> complementaryProducts = new ArrayList<>();

        for (String targetKeyword : targetCategoryKeywords) {
            if (complementaryProducts.size() >= limit) {
                break;
            }
            List<Category> matchedCats = allCategories.stream()
                    .filter(c -> !c.getId().equals(currentProduct.getCategory().getId()))
                    .filter(c -> c.getName().toLowerCase().contains(targetKeyword))
                    .toList();

            for (Category cat : matchedCats) {
                if (complementaryProducts.size() >= limit) {
                    break;
                }
                List<Product> productsInCat = productRepository.findInStockByCategoryId(
                        cat.getId(), PageRequest.of(0, 2));
                for (Product p : productsInCat) {
                    if (!excludedIds.contains(p.getId()) && isInStock(p)) {
                        complementaryProducts.add(p);
                        excludedIds.add(p.getId());
                        if (complementaryProducts.size() >= limit) {
                            break;
                        }
                    }
                }
            }
        }
        return complementaryProducts;
    }

    private String buildInterestQuery(List<Product> interactedProducts,
                                     List<String> recentSearchKeywords,
                                     Product currentProduct) {
        List<String> interests = new ArrayList<>();
        interests.add(productText(currentProduct));
        if (currentProduct.getCategory() != null) {
            interests.add(currentProduct.getCategory().getName());
            if (StringUtils.hasText(currentProduct.getCategory().getDescription())) {
                interests.add(currentProduct.getCategory().getDescription());
            }
        }

        recentSearchKeywords.forEach(interests::add);

        // Include top-scored interacted products — vector similarity threshold filters out noise
        interactedProducts.forEach(interacted -> interests.add(productText(interacted)));

        return String.join(" ", interests);
    }

    private boolean isInStock(Product product) {
        return product.getQuantity() != null && product.getQuantity() > 0;
    }

    private String productText(Product product) {
        return StringUtils.hasText(product.getDescription())
                ? product.getName() + " " + product.getDescription()
                : product.getName();
    }

    private static final Set<String> SMART_HOME_KEYWORDS = Set.of("gia dụng", "nồi chiên", "máy lọc", "robot hút bụi");

    /**
     * Smart home products must NEVER mix with personal IT/electronics products.
     */
    private boolean isCategoryCompatible(Product currentProduct, Product candidate) {
        if (currentProduct.getCategory() == null || candidate.getCategory() == null) {
            return true;
        }
        String currentCat = currentProduct.getCategory().getName().toLowerCase();
        String candidateCat = candidate.getCategory().getName().toLowerCase();

        boolean currentIsSmartHome = SMART_HOME_KEYWORDS.stream().anyMatch(currentCat::contains);
        boolean candidateIsSmartHome = SMART_HOME_KEYWORDS.stream().anyMatch(candidateCat::contains);

        return currentIsSmartHome == candidateIsSmartHome;
    }

    private List<Product> findVectorRecommendations(String query, Set<Long> excludedProductIds) {
        return findVectorRecommendations(query, excludedProductIds, null);
    }

    private List<Product> findVectorRecommendations(String query, Set<Long> excludedProductIds,
                                                     Product currentProduct) {
        try {
            List<Document> matches = productVectorService.search(
                    query, VECTOR_SEARCH_LIMIT, MIN_RELATED_SIMILARITY);
            Map<Long, Product> productsById = new HashMap<>();
            productRepository.findAllById(matches.stream()
                            .map(this::productIdFrom)
                            .filter(Objects::nonNull)
                            .collect(java.util.stream.Collectors.toSet()))
                    .forEach(candidate -> productsById.put(candidate.getId(), candidate));

            return matches.stream()
                    .map(this::productIdFrom)
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(productsById::get)
                    .filter(Objects::nonNull)
                    .filter(candidate -> !excludedProductIds.contains(candidate.getId()))
                    .filter(candidate -> candidate.getQuantity() != null && candidate.getQuantity() > 0)
                    // Apply category compatibility filter when viewing a specific product
                    .filter(candidate -> currentProduct == null || isCategoryCompatible(currentProduct, candidate))
                    .limit(8)
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("Vector recommendations unavailable; using tracking and category fallback ({})",
                    exception.getClass().getSimpleName());
            return List.of();
        }
    }

    private Long productIdFrom(Document document) {
        Object productId = document.getMetadata().get("productId");
        return productId instanceof Number number ? number.longValue() : null;
    }

    private int weight(TrackingEventType eventType) {
        return switch (eventType) {
            case PRODUCT_VIEW -> 1;
            case ADD_TO_CART_CLICK, CART_UPDATE_CLICK -> 2; // click = mild intent
            case CART_UPDATE -> 3;
            case ADD_TO_CART -> 5;
            case ORDER_PLACED -> PURCHASE_WEIGHT;
            default -> 0;
        };
    }
}
