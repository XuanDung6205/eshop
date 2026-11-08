package org.example.eshop.service;

import org.example.eshop.entity.Category;
import org.example.eshop.exception.NotFoundException;
import org.example.eshop.repository.CategoryRepository;
import org.example.eshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục id = " + id));
    }

    @PreAuthorize("hasRole('SELLER')")
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    @PreAuthorize("hasRole('SELLER')")
    public void delete(Long id) {
        if (productRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Danh mục đang có sản phẩm, không thể xoá");
        }
        categoryRepository.deleteById(id);
    }
}

