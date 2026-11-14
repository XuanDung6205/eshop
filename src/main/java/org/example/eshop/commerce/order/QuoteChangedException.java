package org.example.eshop.commerce.order;
public class QuoteChangedException extends RuntimeException {
    private final CheckoutQuote quote;
    public QuoteChangedException(CheckoutQuote quote){super("Báo giá đã thay đổi. Vui lòng kiểm tra lại trước khi đặt hàng.");this.quote=quote;}
    public CheckoutQuote getQuote(){return quote;}
}
