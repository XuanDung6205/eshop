package org.example.eshop.commerce.cart;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.eshop.commerce.port.CurrentActorPort;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/cart")
public class CartApiController {
    private final DbCartService service;private final CurrentActorPort actors;
    public CartApiController(DbCartService service,CurrentActorPort actors){this.service=service;this.actors=actors;}
    public record AddItem(@NotNull @Positive Long variantId,@NotNull @Min(1) @Max(1000000) Integer quantity){}
    public record UpdateItem(@NotNull @Min(1) @Max(1000000) Integer quantity){}
    @GetMapping public CartView get(){return service.get();}
    @PostMapping("/items") public CartView add(@Valid @RequestBody AddItem request){return service.add(request.variantId(),request.quantity());}
    @PatchMapping("/items/{id}") public CartView update(@PathVariable Long id,@Valid @RequestBody UpdateItem request){return service.update(id,request.quantity());}
    @DeleteMapping("/items/{id}") public CartView remove(@PathVariable Long id){return service.remove(id);}
    @DeleteMapping("/items") public CartView clear(){return service.clear();}
    @PostMapping("/merge") public CartView merge(){CartView cart=service.merge();actors.clearGuestIdentity();return cart;}
}
