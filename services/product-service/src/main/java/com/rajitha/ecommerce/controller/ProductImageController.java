package com.rajitha.ecommerce.controller;

import com.rajitha.ecommerce.dto.ProductImageRegisterRequestDTO;
import com.rajitha.ecommerce.dto.ProductImageResponseDTO;
import com.rajitha.ecommerce.dto.ProductImageUploadUrlResponseDTO;
import com.rajitha.ecommerce.service.ProductImageService;
import com.rajitha.ecommerce.util.RolesHeader;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Upload is two steps, not a file upload through this service: the browser asks
// here for a presigned MinIO PUT URL, uploads the bytes directly to MinIO, then
// calls back here to register the resulting object as a ProductImage row. Keeps
// image bytes off this service's request path entirely.
@RestController
@RequestMapping("/api/v1/products/{product-id}/images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @GetMapping
    public ResponseEntity<List<ProductImageResponseDTO>> findByProductId(@PathVariable("product-id") Integer productId) {
        return ResponseEntity.ok(productImageService.findByProductId(productId));
    }

    @PostMapping("/upload-url")
    public ResponseEntity<ProductImageUploadUrlResponseDTO> generateUploadUrl(
            @PathVariable("product-id") Integer productId,
            @RequestParam(defaultValue = "image/jpeg") String contentType,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        return ResponseEntity.ok(
                productImageService.generateUploadUrl(productId, contentType, userId, RolesHeader.isAdmin(roles)));
    }

    @PostMapping
    public ResponseEntity<ProductImageResponseDTO> registerImage(
            @PathVariable("product-id") Integer productId,
            @RequestBody @Valid ProductImageRegisterRequestDTO requestDTO,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        return ResponseEntity.ok(
                productImageService.registerImage(productId, requestDTO.objectKey(), userId, RolesHeader.isAdmin(roles)));
    }

    @DeleteMapping("/{image-id}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable("product-id") Integer productId,
            @PathVariable("image-id") Integer imageId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        productImageService.deleteImage(productId, imageId, userId, RolesHeader.isAdmin(roles));
        return ResponseEntity.noContent().build();
    }
}
