package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@AllArgsConstructor
public class ProductController {
    private final ProductService productService ;

    @PostMapping
    public ResponseEntity<Integer> createProduct(@RequestBody @Valid ProductRequestDTO productRequestDTO) {
        return ResponseEntity.ok(productService.createProduct(productRequestDTO));
    }

    @PostMapping("purchase")
    public ResponseEntity<List<ProductPurchaseResponseDTO>>purchaseProduct(@RequestBody  List<PurchaseRequestDTO> purchaseRequestDTO) {
        return ResponseEntity.ok(productService.purchaseProduct(purchaseRequestDTO));
    }

    @GetMapping("/{product-id}")
    public ResponseEntity<ProductResponseDTO>  productFindById(@PathVariable Integer productId){
        return ResponseEntity.ok(productService.findProductById(productId));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>>  productFindAll(){
        return ResponseEntity.ok(productService.findAllProduct());
    }
}
