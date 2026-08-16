package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;

import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.exeption.ProductAccessDeniedException;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.mapper.ProductMapper;
import com.rajitha.ecommerce.repository.ProductRepository;
import com.rajitha.ecommerce.repository.ProductVariantRepository;
import com.rajitha.ecommerce.service.ProductImageService;
import com.rajitha.ecommerce.service.ProductService;
import com.rajitha.ecommerce.service.ReviewService;
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
    private final ProductImageService productImageService;
    private final ReviewService reviewService;
    public ProductServiceIMPL(ProductRepository productRepository, ProductVariantRepository productVariantRepository, ProductMapper productMapper, ProductImageService productImageService, ReviewService reviewService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productMapper = productMapper;
        this.productImageService = productImageService;
        this.reviewService = reviewService;
    }

    @Override
    public Integer createProduct(ProductRequestDTO productRequestDTO, String sellerId) {
        Product productEntity = productMapper.toProductEntity(productRequestDTO, sellerId);
        return productRepository.save(productEntity).getId();
    }

    @Override
    public void updateProduct(Integer productId, ProductRequestDTO productRequestDTO, String sellerId, boolean isAdmin) {
        var existing = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found" + productId));
        ensureOwnership(existing, sellerId, isAdmin);

        var updated = productMapper.toProductEntity(productRequestDTO, existing.getSellerId());
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPrice(updated.getPrice());
        existing.setCategory(updated.getCategory());

        // Full replace of the variant list: cascade=ALL + orphanRemoval on
        // Product.variants means clearing and re-adding is enough for Hibernate to
        // insert the new ones and delete whatever's no longer present.
        existing.getVariants().clear();
        updated.getVariants().forEach(variant -> variant.setProduct(existing));
        existing.getVariants().addAll(updated.getVariants());

        productRepository.save(existing);
    }

    @Override
    public void deleteProduct(Integer productId, String sellerId, boolean isAdmin) {
        var existing = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found" + productId));
        ensureOwnership(existing, sellerId, isAdmin);
        productRepository.delete(existing);
    }

    private void ensureOwnership(Product product, String sellerId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (product.getSellerId() == null || !product.getSellerId().equals(sellerId)) {
            throw new ProductAccessDeniedException("You do not own product " + product.getId());
        }
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
    public void releaseStock(List<PurchaseRequestDTO> purchaseRequestDTO) {
        for (var request : purchaseRequestDTO) {
            productVariantRepository.findById(request.variantId()).ifPresent(variant -> {
                variant.setAvailableQuantity(variant.getAvailableQuantity() + request.quantity());
                productVariantRepository.save(variant);
            });
        }
    }

    @Override
    public ProductResponseDTO findProductById(Integer productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found" + productId));
        return productMapper.toProductResponseDTO(product, productImageService.findImageUrlsByProductId(productId),
                reviewService.getRatingSummary(productId));
    }

    @Override
    public List<ProductResponseDTO> findAllProduct(String sellerId) {
        var products = sellerId == null || sellerId.isBlank()
                ? productRepository.findAll()
                : productRepository.findAllBySellerId(sellerId);
        return products.stream()
                .map(product -> productMapper.toProductResponseDTO(product, productImageService.findImageUrlsByProductId(product.getId()),
                        reviewService.getRatingSummary(product.getId())))
                .collect(Collectors.toList());
    }
}
