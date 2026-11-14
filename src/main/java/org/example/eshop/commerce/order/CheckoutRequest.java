package org.example.eshop.commerce.order;

import org.example.eshop.commerce.port.CheckoutAddressPort.Address;

public record CheckoutRequest(Long cartId, Long addressId, Address shippingAddress, String couponCode,
                              String paymentMethod, Boolean assemblyRequested, String customerNote,
                              String quoteFingerprint) {}
