package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.entity.Category;
import com.rajitha.ecommerce.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public Product toProductEntity(ProductRequestDTO productRequestDTO) {

        if (productRequestDTO == null) {
            throw new NullPointerException("productRequestDTO is null");
        }
        return Product.builder()
                .id(productRequestDTO.id())
                .name(productRequestDTO.name())
                .description(productRequestDTO.description())
                .availableQuantity(productRequestDTO.availableQuantity())
                .price(productRequestDTO.price())
                .category(Category.builder().id(productRequestDTO.categoryId()).build())
                .build();

}

    public ProductResponseDTO toProductResponseDTO(Product product) {

        if (product == null) {
            throw new NullPointerException("Product is null");
        }
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getAvailableQuantity(),
                product.getPrice(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription()
        );
    }

    public ProductPurchaseResponseDTO toProductPurchaseResponseDTO(Product product,double productQuantity) {

        if (product == null ) {
            throw new NullPointerException("Product is null");
        }
        if (productQuantity <= 0) {
            throw new IllegalArgumentException("Product quantity must be greater than 0");
        }
        return new ProductPurchaseResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                productQuantity
        );
    }
}
