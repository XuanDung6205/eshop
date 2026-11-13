package org.example.eshop.commerce.cart;
import java.util.List;
public record CartView(Long id,long itemCount,String subtotal,String currency,List<Line> items,List<Warning> warnings) {
    public record Line(Long id,Long variantId,Long productId,String sku,String productName,String variantName,String imageUrl,int quantity,String unitPrice,String lineTotal,int availableQuantity,boolean active,List<String> issues){}
    public record Warning(String code,String message,Long itemId){}
}
