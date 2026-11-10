package org.example.eshop.controller;

import lombok.RequiredArgsConstructor;
import org.example.eshop.service.ProductRecommendationService;
import org.example.eshop.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.regex.Pattern;
import org.example.eshop.tracking.TrackingEventRequest;

@Controller
@RequestMapping("/tracking/recommendations")
@RequiredArgsConstructor
public class TrackingRecommendationController {
    private static final Pattern SESSION_ID = Pattern.compile(TrackingEventRequest.UUID_PATTERN);

    private final ProductService productService;
    private final ProductRecommendationService recommendationService;

    @GetMapping
    public String recommendations(@RequestParam Long productId,
                                  @RequestParam(required = false) String sessionId,
                                  Principal principal,
                                  Model model) {
        if (!StringUtils.hasText(sessionId) || !SESSION_ID.matcher(sessionId).matches()) {
            sessionId = null;
        }
        var product = productService.findById(productId);
        model.addAttribute("recommendations", recommendationService.findFor(
                product, principal == null ? null : principal.getName(), sessionId));
        return "shop/detail :: recommendations";
    }
}
