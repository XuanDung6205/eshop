package org.example.eshop.controller;

import org.example.eshop.service.CategoryService;
import org.example.eshop.service.ProductService;
import org.example.eshop.service.ProductRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ShopController {
    private final ProductService productService;
    private final ProductRecommendationService productRecommendationService;
    private final CategoryService categoryService;

    @GetMapping({"/", "/products"})
    public String index(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long categoryId,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        model.addAttribute("products", productService.search(keyword, categoryId, page, 12));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        return "shop/index";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        var product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("recommendations", productRecommendationService.findFor(
                product, principal == null ? null : principal.getName(), null));
        return "shop/detail";
    }
}

