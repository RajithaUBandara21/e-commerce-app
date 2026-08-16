package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.dto.RatingSummaryDTO;
import com.rajitha.ecommerce.dto.ProductVariantRequestDTO;
import com.rajitha.ecommerce.dto.ProductVariantResponseDTO;
import com.rajitha.ecommerce.entity.Category;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {
    public Product toProductEntity(ProductRequestDTO productRequestDTO, String sellerId) {

        if (productRequestDTO == null) {
            throw new NullPointerException("productRequestDTO is null");
        }
        Product product = Product.builder()
                .name(productRequestDTO.name())
                .description(productRequestDTO.description())
                .price(productRequestDTO.price())
                .sellerId(sellerId)
                .category(Category.builder().id(productRequestDTO.categoryId()).build())
                .build();

        List<ProductVariant> variants = productRequestDTO.variants().stream()
                .map(variantDTO -> toProductVariantEntity(variantDTO, product))
                .toList();
        product.setVariants(variants);

        return product;

}

    public ProductVariant toProductVariantEntity(ProductVariantRequestDTO variantRequestDTO, Product product) {
        return ProductVariant.builder()
                .sku(variantRequestDTO.sku())
                .size(variantRequestDTO.size())
                .color(variantRequestDTO.color())
                .availableQuantity(variantRequestDTO.availableQuantity())
                .product(product)
                .build();
    }

    public ProductResponseDTO toProductResponseDTO(Product product, List<String> imageUrls, RatingSummaryDTO ratingSummary) {

        if (product == null) {
            throw new NullPointerException("Product is null");
        }
        List<ProductVariantResponseDTO> variants = product.getVariants() == null ? List.of() :
                product.getVariants().stream().map(this::toProductVariantResponseDTO).toList();
        var rating = ratingSummary == null ? RatingSummaryDTO.EMPTY : ratingSummary;

        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSellerId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription(),
                variants,
                imageUrls == null ? List.of() : imageUrls,
                rating.averageRating(),
                rating.reviewCount()
        );
    }

    public ProductVariantResponseDTO toProductVariantResponseDTO(ProductVariant variant) {
        return ProductVariantResponseDTO.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
                .availableQuantity(variant.getAvailableQuantity())
                .build();
    }

    public ProductPurchaseResponseDTO toProductPurchaseResponseDTO(ProductVariant variant, double variantQuantity) {

        if (variant == null ) {
            throw new NullPointerException("ProductVariant is null");
        }
        if (variantQuantity <= 0) {
            throw new IllegalArgumentException("Product quantity must be greater than 0");
        }
        Product product = variant.getProduct();
        return new ProductPurchaseResponseDTO(
                variant.getId(),
                product.getId(),
                product.getName(),
                product.getDescription(),
                variant.getSize(),
                variant.getColor(),
                product.getPrice(),
                variantQuantity,
                product.getSellerId()
        );
    }
}
