package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.config.MinioProperties;
import com.rajitha.ecommerce.dto.ProductImageResponseDTO;
import com.rajitha.ecommerce.dto.ProductImageUploadUrlResponseDTO;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.entity.ProductImage;
import com.rajitha.ecommerce.exeption.ProductAccessDeniedException;
import com.rajitha.ecommerce.repository.ProductImageRepository;
import com.rajitha.ecommerce.repository.ProductRepository;
import com.rajitha.ecommerce.service.ProductImageService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageServiceIMPL implements ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public ProductImageUploadUrlResponseDTO generateUploadUrl(Integer productId, String contentType, String callerId, boolean isAdmin) {
        var product = findProductOrThrow(productId);
        ensureOwnership(product, callerId, isAdmin);

        var objectKey = "products/" + productId + "/" + UUID.randomUUID() + extensionFor(contentType);

        try {
            var url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioProperties.bucket())
                    .object(objectKey)
                    .expiry(15, TimeUnit.MINUTES)
                    .build());
            return new ProductImageUploadUrlResponseDTO(url, objectKey);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate an image upload URL: " + e.getMessage(), e);
        }
    }

    @Override
    public ProductImageResponseDTO registerImage(Integer productId, String objectKey, String callerId, boolean isAdmin) {
        var product = findProductOrThrow(productId);
        ensureOwnership(product, callerId, isAdmin);

        var position = productImageRepository.countByProduct_Id(productId);
        var image = productImageRepository.save(ProductImage.builder()
                .product(product)
                .objectKey(objectKey)
                .position(position)
                .build());

        return toResponseDTO(image);
    }

    @Override
    public void deleteImage(Integer productId, Integer imageId, String callerId, boolean isAdmin) {
        var product = findProductOrThrow(productId);
        ensureOwnership(product, callerId, isAdmin);

        var image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Product image not found: " + imageId));
        if (image.getProduct() == null || !productId.equals(image.getProduct().getId())) {
            throw new EntityNotFoundException("Image " + imageId + " does not belong to product " + productId);
        }

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.bucket())
                    .object(image.getObjectKey())
                    .build());
        } catch (Exception e) {
            // Don't block the delete on MinIO being unreachable — an orphaned object
            // in the bucket is a cheap cleanup problem, a stuck admin action is not.
            log.warn("Failed to delete MinIO object '{}' :: {}", image.getObjectKey(), e.getMessage());
        }

        productImageRepository.delete(image);
    }

    @Override
    public List<ProductImageResponseDTO> findByProductId(Integer productId) {
        return productImageRepository.findByProduct_IdOrderByPosition(productId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findImageUrlsByProductId(Integer productId) {
        return productImageRepository.findByProduct_IdOrderByPosition(productId).stream()
                .map(this::toUrl)
                .collect(Collectors.toList());
    }

    private Product findProductOrThrow(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found" + productId));
    }

    private void ensureOwnership(Product product, String callerId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (product.getSellerId() == null || !product.getSellerId().equals(callerId)) {
            throw new ProductAccessDeniedException("You do not own product " + product.getId());
        }
    }

    private ProductImageResponseDTO toResponseDTO(ProductImage image) {
        return ProductImageResponseDTO.builder()
                .id(image.getId())
                .url(toUrl(image))
                .position(image.getPosition())
                .build();
    }

    private String toUrl(ProductImage image) {
        return minioProperties.publicBaseUrl() + "/" + minioProperties.bucket() + "/" + image.getObjectKey();
    }

    private String extensionFor(String contentType) {
        if (contentType == null) {
            return "";
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
