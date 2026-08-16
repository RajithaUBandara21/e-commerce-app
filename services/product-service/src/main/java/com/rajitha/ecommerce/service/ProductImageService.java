package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.ProductImageResponseDTO;
import com.rajitha.ecommerce.dto.ProductImageUploadUrlResponseDTO;

import java.util.List;

public interface ProductImageService {

    ProductImageUploadUrlResponseDTO generateUploadUrl(Integer productId, String contentType, String callerId, boolean isAdmin);

    ProductImageResponseDTO registerImage(Integer productId, String objectKey, String callerId, boolean isAdmin);

    void deleteImage(Integer productId, Integer imageId, String callerId, boolean isAdmin);

    List<ProductImageResponseDTO> findByProductId(Integer productId);

    // Plain URLs, ordered — what ProductService embeds in ProductResponseDTO.
    // Kept here (not duplicated in ProductServiceIMPL) since this is the one place
    // that knows how a ProductImage's objectKey becomes a public URL.
    List<String> findImageUrlsByProductId(Integer productId);
}
