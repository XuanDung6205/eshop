package org.example.eshop.commerce.promotion;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
@RestController @RequestMapping("/api/admin/coupons")
public class CouponAdminController {
    private final CouponAdminService service;
    public CouponAdminController(CouponAdminService service){this.service=service;}
    @GetMapping public CouponAdminService.CouponPage list(@RequestParam(defaultValue="")String query,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.list(query,page,size);}
    @GetMapping("/{id}") public CouponView get(@PathVariable Long id){return service.get(id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public CouponView create(@RequestBody JsonNode body){return service.create(body);}
    @PatchMapping("/{id}") public CouponView patch(@PathVariable Long id,@RequestBody JsonNode body){return service.patch(id,body);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id){service.delete(id);}
    @PostMapping("/{id}/preview") public CouponAdminService.Preview preview(@PathVariable Long id,@RequestBody JsonNode body){return service.preview(id,body);}
}
