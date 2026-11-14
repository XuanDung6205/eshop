package org.example.eshop.commerce.order;

import org.example.eshop.commerce.port.CheckoutAddressPort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutApiController {
    private final CheckoutQuoteService quotes;private final CheckoutService checkout;private final CheckoutAddressPort addresses;
    public CheckoutApiController(CheckoutQuoteService quotes,CheckoutService checkout,CheckoutAddressPort addresses){this.quotes=quotes;this.checkout=checkout;this.addresses=addresses;}
    @GetMapping("/addresses") public Object addresses(){return Map.of("items",addresses.findByUserId(quotes.actorId()));}
    @PostMapping("/quote") public CheckoutQuote quote(@RequestBody CheckoutRequest request){return quotes.quote(request);}
    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED)
    public Object order(@RequestBody CheckoutRequest request,@RequestHeader(value="Idempotency-Key",required=false) String key){return checkout.place(request,key);}
    @ExceptionHandler(QuoteChangedException.class) public ResponseEntity<?> changed(QuoteChangedException e){return ResponseEntity.status(409).body(Map.of("code","QUOTE_CHANGED","message",e.getMessage(),"quote",e.getQuote()));}
}
