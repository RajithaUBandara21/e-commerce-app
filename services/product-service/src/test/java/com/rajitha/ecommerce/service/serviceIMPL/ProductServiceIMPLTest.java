package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.ProductVariantRequestDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.entity.Category;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.entity.ProductVariant;
import com.rajitha.ecommerce.exeption.ProductAccessDeniedException;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.mapper.ProductMapper;
import com.rajitha.ecommerce.repository.ProductRepository;
import com.rajitha.ecommerce.repository.ProductVariantRepository;
import com.rajitha.ecommerce.service.ProductImageService;
import com.rajitha.ecommerce.service.ReviewService;
import com.rajitha.ecommerce.dto.RatingSummaryDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceIMPLTest {

    @InjectMocks
    private ProductServiceIMPL productServiceIMPL;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductImageService productImageService;

    @Mock
    private ReviewService reviewService;

    @Test
    void shouldSuccessfullyCreateProduct() {

        ProductVariantRequestDTO variantDTO = ProductVariantRequestDTO.builder()
                .id(1)
                .sku("TS-RED-M")
                .size("M")
                .color("Red")
                .availableQuantity(1256)
                .build();

        ProductRequestDTO dto = ProductRequestDTO.builder()
                .id(1)
                .name("Test name")
                .description("Test description")
                .price(new BigDecimal("256849"))
                .categoryId(256)
                .variants(List.of(variantDTO))
                .build();

        Product product = Product.builder()
                .id(1)
                .name("Test name")
                .description("Test description")
                .price(new BigDecimal("256849"))
                .category(Category.builder().id(1).build())
                .build();

        Mockito.when(productMapper.toProductEntity(dto, "seller-1")).thenReturn(product);
        Mockito.when(productRepository.save(Mockito.any(Product.class))).thenReturn(product);

        Integer id = productServiceIMPL.createProduct(dto, "seller-1");

        assertNotNull(id);
        assertEquals(1, id);

        Mockito.verify(productMapper,Mockito.times(1)).toProductEntity(dto, "seller-1");

        Mockito.verify(productRepository,Mockito.times(1)).save(Mockito.any(Product.class));
    }

    @Test
    void shouldSuccessfullyPurchaseProductService(){

        List<PurchaseRequestDTO> requests = List.of(
                new PurchaseRequestDTO(1, 2.0),
                new PurchaseRequestDTO(2, 1.0)
        );

        Product product1 = Product.builder().id(1).name("name1").description("description1").price(new BigDecimal(265)).category(Category.builder().id(1).name("categoryName1").build()).build();
        Product product2 = Product.builder().id(2).name("name2").description("description2").price(new BigDecimal(262)).category(Category.builder().id(2).name("categoryName2").build()).build();

        ProductVariant variant1 = ProductVariant.builder().id(1).sku("SKU-1").size("M").color("Red").availableQuantity(65).product(product1).build();
        ProductVariant variant2 = ProductVariant.builder().id(2).sku("SKU-2").size("L").color("Blue").availableQuantity(55).product(product2).build();

        List<ProductVariant> sortedVariants = List.of(
                variant1,
                variant2
        );

        ProductPurchaseResponseDTO dto1 = new ProductPurchaseResponseDTO(1,1,"name1","description1","M","Red", new BigDecimal(265) , 63.0, "seller-1");
        ProductPurchaseResponseDTO dto2 = new ProductPurchaseResponseDTO(2,2,"name2","description2","L","Blue", new BigDecimal(262) , 54.0, "seller-2");




        Mockito.when(productVariantRepository.findAllByIdInOrderById(Mockito.anyList())).thenReturn(sortedVariants);

        Mockito.when(productMapper.toProductPurchaseResponseDTO(Mockito.eq(variant1),Mockito.anyDouble())).thenReturn( dto1);
        Mockito.when(productMapper.toProductPurchaseResponseDTO(Mockito.eq(variant2),Mockito.anyDouble())).thenReturn( dto2);

        List<ProductPurchaseResponseDTO> result =productServiceIMPL.purchaseProductService(requests);

        assertEquals(2, result.size());
        assertEquals(dto1, result.get(0));
        assertEquals(dto2, result.get(1));


    }



    @Test
    void shouldThrowProductNotExistException(){

        List<PurchaseRequestDTO> requests1 = List.of(
                new PurchaseRequestDTO(1, 2.0),
                new PurchaseRequestDTO(2, 1.0)
        );
        List<PurchaseRequestDTO> requests2 = List.of(
                new PurchaseRequestDTO(1, 90.0)
        );

        Product product1 = Product.builder().id(1).name("name1").description("description1").price(new BigDecimal(265)).category(Category.builder().id(1).name("categoryName1").build()).build();
        ProductVariant variant1 = ProductVariant.builder().id(1).sku("SKU-1").size("M").color("Red").availableQuantity(65).product(product1).build();


        List<ProductVariant> sortedVariants = List.of(
                variant1
        );


        Mockito.when(productVariantRepository.findAllByIdInOrderById(Mockito.anyList())).thenReturn(sortedVariants);



var productNotExistException = Assertions.assertThrows(
                ProductPurchaseException.class,
                () -> productServiceIMPL.purchaseProductService(requests1)
        );

        Assertions.assertEquals("One or more product variant not exists",productNotExistException.getMessage());

        var productRequestCountMinusException = Assertions.assertThrows(
                ProductPurchaseException.class,
                () -> productServiceIMPL.purchaseProductService(requests2)
        );

        Assertions.assertEquals( "Insufficient stock quantity for product variant with id"+requests2.get(0).variantId() ,productRequestCountMinusException.getMessage());
    }

    @Test
    void shouldThrowProductPurchaseExceptionOnConcurrentStockUpdate(){

        List<PurchaseRequestDTO> requests = List.of(
                new PurchaseRequestDTO(1, 2.0)
        );

        Product product1 = Product.builder().id(1).name("name1").description("description1").price(new BigDecimal(265)).category(Category.builder().id(1).name("categoryName1").build()).build();
        ProductVariant variant1 = ProductVariant.builder().id(1).sku("SKU-1").size("M").color("Red").availableQuantity(65).product(product1).build();

        List<ProductVariant> sortedVariants = List.of(variant1);

        Mockito.when(productVariantRepository.findAllByIdInOrderById(Mockito.anyList())).thenReturn(sortedVariants);
        Mockito.when(productVariantRepository.save(Mockito.any(ProductVariant.class)))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(ProductVariant.class, 1));

        var exception = Assertions.assertThrows(
                ProductPurchaseException.class,
                () -> productServiceIMPL.purchaseProductService(requests)
        );

        Assertions.assertEquals("Stock for product variant with id1 changed concurrently, please retry", exception.getMessage());
    }

    @Test
    void shouldReleaseStockForExistingVariants(){

        ProductVariant variant = ProductVariant.builder().id(1).sku("SKU-1").size("M").color("Red").availableQuantity(3).build();

        Mockito.when(productVariantRepository.findById(1)).thenReturn(java.util.Optional.of(variant));

        productServiceIMPL.releaseStock(List.of(new PurchaseRequestDTO(1, 2.0)));

        Assertions.assertEquals(5, variant.getAvailableQuantity());
        Mockito.verify(productVariantRepository, Mockito.times(1)).save(variant);
    }

    @Test
    void shouldSkipReleaseForUnknownVariant(){

        Mockito.when(productVariantRepository.findById(99)).thenReturn(java.util.Optional.empty());

        productServiceIMPL.releaseStock(List.of(new PurchaseRequestDTO(99, 2.0)));

        Mockito.verify(productVariantRepository, Mockito.never()).save(Mockito.any(ProductVariant.class));
    }

    @Test
    void shouldRejectUpdateFromNonOwningSeller() {
        Product product = Product.builder().id(1).sellerId("seller-1").build();
        Mockito.when(productRepository.findById(1)).thenReturn(java.util.Optional.of(product));

        ProductRequestDTO dto = ProductRequestDTO.builder()
                .name("New name").description("desc").price(new BigDecimal("10"))
                .categoryId(1).variants(List.of()).build();

        Assertions.assertThrows(ProductAccessDeniedException.class,
                () -> productServiceIMPL.updateProduct(1, dto, "seller-2", false));

        Mockito.verify(productRepository, Mockito.never()).save(Mockito.any(Product.class));
    }

    @Test
    void shouldAllowAdminToUpdateAnyProduct() {
        Product product = Product.builder().id(1).sellerId("seller-1")
                .variants(new java.util.ArrayList<>()).build();
        Mockito.when(productRepository.findById(1)).thenReturn(java.util.Optional.of(product));

        ProductVariantRequestDTO variantDTO = ProductVariantRequestDTO.builder()
                .sku("SKU-1").size("M").color("Red").availableQuantity(5).build();
        ProductRequestDTO dto = ProductRequestDTO.builder()
                .name("New name").description("desc").price(new BigDecimal("10"))
                .categoryId(1).variants(List.of(variantDTO)).build();

        // productMapper is mocked (@InjectMocks wires it into productServiceIMPL), so
        // stub it with what a real ProductMapper would actually produce for this DTO.
        Mockito.when(productMapper.toProductEntity(dto, "seller-1"))
                .thenReturn(new ProductMapper().toProductEntity(dto, "seller-1"));

        productServiceIMPL.updateProduct(1, dto, "some-admin", true);

        Mockito.verify(productRepository).save(product);
        assertEquals("New name", product.getName());
        assertEquals(1, product.getVariants().size());
    }

    @Test
    void shouldRejectDeleteFromNonOwningSeller() {
        Product product = Product.builder().id(1).sellerId("seller-1").build();
        Mockito.when(productRepository.findById(1)).thenReturn(java.util.Optional.of(product));

        Assertions.assertThrows(ProductAccessDeniedException.class,
                () -> productServiceIMPL.deleteProduct(1, "seller-2", false));

        Mockito.verify(productRepository, Mockito.never()).delete(Mockito.any(Product.class));
    }

    @Test
    void shouldAllowOwningSellerToDelete() {
        Product product = Product.builder().id(1).sellerId("seller-1").build();
        Mockito.when(productRepository.findById(1)).thenReturn(java.util.Optional.of(product));

        productServiceIMPL.deleteProduct(1, "seller-1", false);

        Mockito.verify(productRepository).delete(product);
    }

    @Test
    void shouldIncludeImageUrlsWhenFindingProductById() {
        Product product = Product.builder().id(1).sellerId("seller-1")
                .category(Category.builder().id(1).build()).build();
        var imageUrls = List.of("http://localhost:9000/product-images/products/1/a.jpg");
        var responseDTO = com.rajitha.ecommerce.dto.ProductResponseDTO.builder().id(1).imageUrls(imageUrls).build();

        Mockito.when(productRepository.findById(1)).thenReturn(java.util.Optional.of(product));
        Mockito.when(productImageService.findImageUrlsByProductId(1)).thenReturn(imageUrls);
        Mockito.when(reviewService.getRatingSummary(1)).thenReturn(RatingSummaryDTO.EMPTY);
        Mockito.when(productMapper.toProductResponseDTO(product, imageUrls, RatingSummaryDTO.EMPTY)).thenReturn(responseDTO);

        var result = productServiceIMPL.findProductById(1);

        assertEquals(imageUrls, result.imageUrls());
    }
}
