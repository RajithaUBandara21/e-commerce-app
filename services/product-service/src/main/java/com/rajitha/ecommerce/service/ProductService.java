package com.rajitha.ecommerce.service;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import jakarta.validation.Valid;

import java.util.List;

public interface ProductService {

    Integer createProduct(@Valid ProductRequestDTO productRequestDTO);

    List<ProductPurchaseResponseDTO> purchaseProductService(List<PurchaseRequestDTO> purchaseRequestDTO);

    List<ProductPurchaseResponseDTO> purchaseProduct(List<PurchaseRequestDTO> purchaseRequestDTO);

    ProductResponseDTO findProductById(Integer productId);

    List<ProductResponseDTO> findAllProduct();
}
