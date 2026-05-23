package ma.ensa.commandes_service.service;

import ma.ensa.commandes_service.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("ADMIN")
public interface ProductService {
    @GetMapping("/products/{id}")
    ProductDto getProductById(@PathVariable Long id);
}
