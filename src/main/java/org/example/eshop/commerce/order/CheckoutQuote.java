package org.example.eshop.commerce.order;

import org.example.eshop.commerce.port.CheckoutAddressPort.Address;
import java.util.List;

public record CheckoutQuote(long cartId, String quoteFingerprint, String currency, List<Line> items,
        Address shippingAddress, String subtotal, String discountAmount, String shippingFee,
        String assemblyFee, String taxAmount, String grandTotal, String couponCode,
        String paymentMethod, boolean assemblyRequested, List<String> warnings) {
    public record Line(long variantId,long productId,String sku,String productName,String variantName,
        String imageUrl,String colorName,String dimensionsSnapshot,String materialsSnapshot,int warrantyMonths,
        boolean assemblyRequired,int quantity,String unitPrice,String lineTotal,int availableQuantity) {}
}
