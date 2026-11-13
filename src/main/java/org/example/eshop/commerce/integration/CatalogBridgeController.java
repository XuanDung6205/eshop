package org.example.eshop.commerce.integration;

import org.example.eshop.commerce.common.CommerceException;
import org.example.eshop.commerce.port.InventoryPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class CatalogBridgeController {
    private final JdbcCatalogAdapter catalog;
    private final InventoryPort inventory;
    public CatalogBridgeController(JdbcCatalogAdapter catalog,InventoryPort inventory) { this.catalog=catalog; this.inventory=inventory; }
    public record VariantView(Long id,Long productId,String sku,String productName,String variantName,String price,String imageUrl,boolean active,int availableQuantity) {}
    public record CatalogPage(List<VariantView> items,int page,int size,long totalElements,int totalPages) {}
    @GetMapping("/api/commerce/catalog/variants")
    public CatalogPage variants(@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size) {
        if(page<0 || size<1 || size>100) throw CommerceException.badRequest("INVALID_PAGE","Page must be nonnegative and size must be between 1 and 100.");
        query=query==null ? "" : query.trim();
        if(query.length()>200) throw CommerceException.badRequest("INVALID_QUERY","Search text must be at most 200 characters.");
        var result=catalog.search(query,page,size);
        var ids=result.items().stream().map(v -> v.id()).collect(Collectors.toSet());
        var availability=inventory.availableQuantities(ids);
        var items=result.items().stream().map(v -> new VariantView(v.id(),v.productId(),v.sku(),v.productName(),v.variantName(),v.price().setScale(2).toPlainString(),v.imageUrl(),v.active(),availability.getOrDefault(v.id(),0))).toList();
        int totalPages=(int)Math.min(Integer.MAX_VALUE,(result.totalElements()+size-1)/size);
        return new CatalogPage(items,page,size,result.totalElements(),totalPages);
    }
}
