package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;

import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.mapper.ProductMapper;
import com.rajitha.ecommerce.repository.ProductRepository;
import com.rajitha.ecommerce.repository.ProductVariantRepository;
import com.rajitha.ecommerce.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceIMPL implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductMapper productMapper;
    public ProductServiceIMPL(ProductRepository productRepository, ProductVariantRepository productVariantRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productMapper = productMapper;
    }

    @Override
    public Integer createProduct(ProductRequestDTO productRequestDTO) {
        Product productEntity = productMapper.toProductEntity(productRequestDTO);
        return productRepository.save(productEntity).getId();
    }

    @Override
    public List<ProductPurchaseResponseDTO> purchaseProductService(List<PurchaseRequestDTO> purchaseRequestDTO) {

        var variantIds = purchaseRequestDTO.stream().map(PurchaseRequestDTO :: variantId).toList();

            var sortedVariants  = productVariantRepository.findAllByIdInOrderById(variantIds);


        if(variantIds.size() != sortedVariants.size()){
            throw new ProductPurchaseException("One or more product variant not exists");
        }

        var sortedRequest = purchaseRequestDTO.stream().sorted(Comparator.comparing(PurchaseRequestDTO::variantId)).toList();

        var purchasedProducts = new ArrayList<ProductPurchaseResponseDTO>();


        for (int i = 0 ; i < sortedVariants.size() ; i++){
            var variant = sortedVariants.get(i);
            var variantRequest  = sortedRequest.get(i);
            if (variant.getAvailableQuantity() < variantRequest.quantity()){
                throw new ProductPurchaseException("Insufficient stock quantity for product variant with id"+variant.getId());
            }
            var newAvailableQuantity = variant.getAvailableQuantity() - variantRequest.quantity();
            variant.setAvailableQuantity(newAvailableQuantity);
            try {
                productVariantRepository.save(variant);
            } catch (OptimisticLockingFailureException e) {
                throw new ProductPurchaseException("Stock for product variant with id" + variant.getId() + " changed concurrently, please retry");
            }
            purchasedProducts.add(productMapper.toProductPurchaseResponseDTO(variant, variantRequest.quantity()));
        }

        return purchasedProducts;
    }

    @Override
    public List<ProductPurchaseResponseDTO> purchaseProduct(List<PurchaseRequestDTO> purchaseRequestDTO) {
        return List.of();
    }

    @Override
    public ProductResponseDTO findProductById(Integer productId) {
        return productRepository.findById(productId).map(productMapper::toProductResponseDTO).orElseThrow(()-> new EntityNotFoundException("Product not found" + productId));
    }

    @Override
    public List<ProductResponseDTO> findAllProduct() {
        return productRepository.findAll().stream().map(productMapper::toProductResponseDTO).collect(Collectors.toList());
    }
}
