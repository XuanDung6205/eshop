package org.example.eshop.commerce.order;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class CustomerOrderApiController {
    private final OrderQueryService orders;private final OrderCancellationService cancellation;
    public CustomerOrderApiController(OrderQueryService orders,OrderCancellationService cancellation){this.orders=orders;this.cancellation=cancellation;}
    @GetMapping public Object list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="") String status){return orders.list(page,size,status);}
    @GetMapping("/{id}") public Object detail(@PathVariable long id){return orders.detail(id);}
    public record CancelRequest(String reason){}
    @PostMapping("/{id}/cancel") public Object cancel(@PathVariable long id,@RequestBody CancelRequest r){return cancellation.cancel(id,r.reason());}
}
