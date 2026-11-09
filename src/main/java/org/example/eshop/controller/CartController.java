package org.example.eshop.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.eshop.cart.Cart;
import org.example.eshop.entity.Product;
import org.example.eshop.service.ProductService;
import org.example.eshop.tracking.TrackingEventType;
import org.example.eshop.tracking.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final Cart cart;
    private final ProductService productService;
    private final TrackingService trackingService;

    @GetMapping
    public String view(Model model) {
        model.addAttribute("cart", cart);
        return "cart/view";
    }

    @PostMapping("/add")
    public String add(@RequestParam Long productId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(required = false) String trackingSessionId,
                      HttpServletRequest request, Principal principal, RedirectAttributes ra) {
        Product p = productService.findById(productId);
        int wanted = cart.getQuantityOf(productId) + quantity;
        if (quantity < 1 || wanted > p.getQuantity()) {
            ra.addFlashAttribute("error", "Không đủ hàng: \"" + p.getName() + "\" chỉ còn " + p.getQuantity());
            return "redirect:/products/" + productId;
        }
        cart.add(p, quantity);
        trackingService.recordCartEvent(TrackingEventType.ADD_TO_CART, productId, quantity,
                trackingSessionId, request, principal);
        ra.addFlashAttribute("message", "Đã thêm \"" + p.getName() + "\" vào giỏ hàng");
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String update(@RequestParam Long productId, @RequestParam int quantity,
                         @RequestParam(required = false) String trackingSessionId,
                         HttpServletRequest request, Principal principal, RedirectAttributes ra) {
        Product p = productService.findById(productId);
        int previous = cart.getQuantityOf(productId);
        if (quantity > p.getQuantity()) {
            ra.addFlashAttribute("error", "\"" + p.getName() + "\" chỉ còn " + p.getQuantity());
        } else {
            cart.update(productId, quantity);
            if (previous > 0 && previous != quantity) {
                trackingService.recordCartEvent(quantity <= 0 ? TrackingEventType.CART_REMOVE : TrackingEventType.CART_UPDATE,
                        productId, quantity, trackingSessionId, request, principal);
            }
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{productId}")
    public String remove(@PathVariable Long productId, @RequestParam(required = false) String trackingSessionId,
                         HttpServletRequest request, Principal principal) {
        int previous = cart.getQuantityOf(productId);
        cart.remove(productId);
        if (previous > 0) {
            trackingService.recordCartEvent(TrackingEventType.CART_REMOVE, productId, previous,
                    trackingSessionId, request, principal);
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear(@RequestParam(required = false) String trackingSessionId,
                        HttpServletRequest request, Principal principal) {
        int previous = cart.getTotalQuantity();
        cart.clear();
        if (previous > 0) {
            trackingService.recordCartEvent(TrackingEventType.CART_CLEAR, null, previous,
                    trackingSessionId, request, principal);
        }
        return "redirect:/cart";
    }
}
