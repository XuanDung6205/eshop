package org.example.eshop.service;

import org.example.eshop.entity.Category;
import org.example.eshop.entity.Product;
import org.example.eshop.repository.CategoryRepository;
import org.example.eshop.repository.ProductRepository;
import org.example.eshop.service.ai.ProductVectorService;
import org.springframework.ai.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ProductVectorService productVectorService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        productVectorService = mock(ProductVectorService.class);
        productService = new ProductService(productRepository, categoryRepository, productVectorService);
    }

    @Test
    void savesProductAndIndexesSavedRecord() {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 12L);
        Category category = mock(Category.class);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(productRepository.save(product)).thenReturn(product);

        Product saved = productService.save(product, 3L);

        assertEquals(product, saved);
        verify(productVectorService).indexProduct(saved);
    }

    @Test
    void deletesProductFromMariaDbBeforeRemovingItsVectors() {
        Product product = new Product();
        when(productRepository.findById(12L)).thenReturn(Optional.of(product));

        productService.delete(12L);

        var inOrder = inOrder(productRepository, productVectorService);
        inOrder.verify(productRepository).delete(product);
        inOrder.verify(productRepository).flush();
        inOrder.verify(productVectorService).deleteProduct(12L);
    }

    @Test
    void doesNotDeleteVectorsWhenMariaDbRejectsProductDelete() {
        Product product = new Product();
        when(productRepository.findById(12L)).thenReturn(Optional.of(product));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("referenced"))
                .when(productRepository).flush();

        assertThrows(IllegalStateException.class, () -> productService.delete(12L));
        verify(productVectorService, never()).deleteProduct(12L);
    }

    @Test
    void reindexesAllExistingDatabaseProducts() {
        List<Product> products = List.of(new Product(), new Product());
        when(productRepository.findAll()).thenReturn(products);

        assertEquals(2, productService.reindexAllProducts());
        verify(productVectorService).indexProducts(products);
    }

    @Test
    void shopSearchUsesVectorsAndKeepsCategoryFilter() {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 12L);
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(3L);
        product.setCategory(category);
        Product otherCategoryProduct = new Product();
        ReflectionTestUtils.setField(otherCategoryProduct, "id", 13L);
        Category otherCategory = mock(Category.class);
        when(otherCategory.getId()).thenReturn(4L);
        otherCategoryProduct.setCategory(otherCategory);
        Document categoryMatch = Document.builder().id("phone-name").text("Phone A")
                .metadata(Map.of("productId", 12L, "sourceType", "NAME"))
                .score(0.94).build();
        Document otherCategoryMatch = Document.builder().id("other-phone-name").text("Phone B")
                .metadata(Map.of("productId", 13L, "sourceType", "NAME"))
                .score(0.93).build();
        when(productVectorService.search("phone camera", 100))
                .thenReturn(List.of(categoryMatch, otherCategoryMatch));
        when(productRepository.findAllById(List.of(12L, 13L)))
                .thenReturn(List.of(product, otherCategoryProduct));

        var results = productService.searchForShop(" phone camera ", 3L, 0, 12);

        assertEquals(List.of(product), results.getContent());
        assertEquals(1, results.getTotalElements());
        verify(productVectorService).search("phone camera", 100);
        verify(productRepository, never()).search(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void semanticSearchReturnsRankedProductsOnceEvenWhenNameAndDescriptionBothMatch() {
        Product first = new Product();
        ReflectionTestUtils.setField(first, "id", 12L);
        Product second = new Product();
        ReflectionTestUtils.setField(second, "id", 13L);
        Document firstName = Document.builder().id("first-name").text("Phone A")
                .metadata(Map.of("productId", 12L, "sourceType", "NAME"))
                .score(0.94).build();
        Document firstDescription = Document.builder().id("first-description").text("Good camera")
                .metadata(Map.of("productId", 12L, "sourceType", "DESCRIPTION"))
                .score(0.89).build();
        Document secondName = Document.builder().id("second-name").text("Phone B")
                .metadata(Map.of("productId", 13L, "sourceType", "NAME"))
                .score(0.83).build();
        when(productVectorService.search("phone camera", 5))
                .thenReturn(List.of(firstName, firstDescription, secondName));
        when(productRepository.findAllById(List.of(12L, 13L))).thenReturn(List.of(first, second));

        List<ProductService.SemanticSearchResult> results = productService.semanticSearch("phone camera", 5);

        assertEquals(2, results.size());
        assertEquals(first, results.get(0).product());
        assertEquals(0.94, results.get(0).score());
        assertEquals("NAME", results.get(0).matchedField());
        assertEquals(second, results.get(1).product());
    }

    @Test
    void shopSearchFiltersOutLowSimilarityAndUnrelatedMentionedCategories() {
        Product phone = new Product();
        ReflectionTestUtils.setField(phone, "id", 1L);
        Category phoneCat = mock(Category.class);
        when(phoneCat.getId()).thenReturn(1L);
        phone.setCategory(phoneCat);

        Product lowScorePhone = new Product();
        ReflectionTestUtils.setField(lowScorePhone, "id", 2L);
        lowScorePhone.setCategory(phoneCat);

        Product vacuum = new Product();
        ReflectionTestUtils.setField(vacuum, "id", 3L);
        Category homeCat = mock(Category.class);
        when(homeCat.getId()).thenReturn(15L);
        vacuum.setCategory(homeCat);

        Document phoneDoc = Document.builder().id("p1").text("iPhone 15")
                .metadata(Map.of("productId", 1L, "sourceType", "NAME")).score(0.75).build();
        Document lowScoreDoc = Document.builder().id("p2").text("Phone Case")
                .metadata(Map.of("productId", 2L, "sourceType", "NAME")).score(0.45).build();
        Document vacuumDoc = Document.builder().id("p3").text("Robot hút bụi")
                .metadata(Map.of("productId", 3L, "sourceType", "NAME")).score(0.65).build();

        when(productRepository.findCategoryIdsMentionedInQuery("điện thoại xấu")).thenReturn(List.of(1L));
        when(productVectorService.search("điện thoại xấu", 20))
                .thenReturn(List.of(phoneDoc, lowScoreDoc, vacuumDoc));
        when(productRepository.findAllById(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(phone, lowScorePhone, vacuum));

        var results = productService.searchForShop("điện thoại xấu", null, 0, 12);

        assertEquals(List.of(phone), results.getContent());
    }

    @Test
    void shopSearchCombinesKeywordMatchesCaseInsensitivelyWithVectors() {
        Product speaker1 = new Product();
        ReflectionTestUtils.setField(speaker1, "id", 10L);
        Product speaker2 = new Product();
        ReflectionTestUtils.setField(speaker2, "id", 11L);
        Product speaker3 = new Product();
        ReflectionTestUtils.setField(speaker3, "id", 12L);

        when(productRepository.findKeywordMatches("loa", null))
                .thenReturn(List.of(speaker1, speaker2, speaker3));
        when(productVectorService.search("loa", 20)).thenReturn(List.of());

        var results = productService.searchForShop("loa", null, 0, 12);

        assertEquals(List.of(speaker1, speaker2, speaker3), results.getContent());
    }
}
