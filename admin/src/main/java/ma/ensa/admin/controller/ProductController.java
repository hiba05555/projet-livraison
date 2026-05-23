package ma.ensa.admin.controller;


import jakarta.ws.rs.PathParam;
import ma.ensa.admin.entity.Product;
import ma.ensa.admin.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {


    @Autowired
    ProductRepository productRepository;


    @GetMapping
    public List<Product> findall() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getProductById(@PathVariable Long id){
        return ResponseEntity.ok(productRepository.findById(id));
    }

    @PostMapping("/addProduct/{id}")
    public ResponseEntity<Object> addProduct(@PathVariable Long id, @RequestBody Product product){
        return ResponseEntity.ok(productRepository.save(product));

    }

}
