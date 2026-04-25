package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;

import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.mapper.ProductMapper;
import com.rajitha.ecommerce.repository.ProductRepository;
import com.rajitha.ecommerce.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceIMPL implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    public ProductServiceIMPL(ProductRepository productRepository,ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    public Integer createProduct(ProductRequestDTO productRequestDTO) {
        Product productEntity = productMapper.toProductEntity(productRequestDTO);
        return productRepository.save(productEntity).getId();
    }

    @Override
    public List<ProductPurchaseResponseDTO> purchaseProductService(List<PurchaseRequestDTO> purchaseRequestDTO) {

        var productIds = purchaseRequestDTO.stream().map(PurchaseRequestDTO :: productId).toList();
        var sortedProduct  = productRepository.findAllByIdInOrderById(productIds);


        if(productIds.size() != sortedProduct.size()){
            throw new ProductPurchaseException("One or more product not exists");
        }
        var sortedRequest = purchaseRequestDTO.stream().sorted(Comparator.comparing(PurchaseRequestDTO::productId)).toList();
        var purchasedProducts = new ArrayList<ProductPurchaseResponseDTO>();
        for (int i = 0 ; i < sortedProduct.size() ; i++){
            var product = sortedProduct.get(i);
            var productRequest  = sortedRequest.get(i);
            if (product.getAvailableQuantity() < productRequest.quantity()){
                throw new ProductPurchaseException("Insufficient stock quantity for product with id "+product.getId());
            }
            var newAvailableQuantity = product.getAvailableQuantity() - productRequest.quantity();
            product.setAvailableQuantity(newAvailableQuantity);
            productRepository.save(product);
            purchasedProducts.add(productMapper.toProductPurchaseResponseDTO(product, productRequest.quantity()));
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
