package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.service.ProductService;
import com.rajitha.ecommerce.util.RolesHeader;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// X-User-Id/X-User-Roles are set by api-gateway from the caller's validated JWT and
// stripped from any inbound client request first — mutation endpoints trust these
// headers rather than re-validating the JWT themselves (gateway-as-trust-boundary,
// see PLAN.md). Anonymous GETs never carry them, which is fine since reads are public.
@RestController
@RequestMapping("/api/v1/products")
@AllArgsConstructor
public class ProductController {
    private final ProductService productService ;

    @PostMapping
    public ResponseEntity<Integer> createProduct(
            @RequestBody @Valid ProductRequestDTO productRequestDTO,
            @RequestHeader("X-User-Id") String sellerId) {
        return ResponseEntity.ok(productService.createProduct(productRequestDTO, sellerId));
    }

    @PutMapping("/{product-id}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable("product-id") Integer productId,
            @RequestBody @Valid ProductRequestDTO productRequestDTO,
            @RequestHeader("X-User-Id") String sellerId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        productService.updateProduct(productId, productRequestDTO, sellerId, RolesHeader.isAdmin(roles));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{product-id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable("product-id") Integer productId,
            @RequestHeader("X-User-Id") String sellerId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        productService.deleteProduct(productId, sellerId, RolesHeader.isAdmin(roles));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/purchase")
    public ResponseEntity<List<ProductPurchaseResponseDTO>>purchaseProduct(@RequestBody  List<PurchaseRequestDTO> purchaseRequestDTO) {
        return ResponseEntity.ok(productService.purchaseProductService(purchaseRequestDTO));
    }

    @GetMapping("/{product-id}")
    public ResponseEntity<ProductResponseDTO>  productFindById(@PathVariable("product-id") Integer productId){
        return ResponseEntity.ok(productService.findProductById(productId));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>>  productFindAll(@RequestParam(required = false) String sellerId){
        return ResponseEntity.ok(productService.findAllProduct(sellerId));
    }
}
