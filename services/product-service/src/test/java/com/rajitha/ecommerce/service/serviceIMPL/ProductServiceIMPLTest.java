package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.ProductVariantRequestDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.entity.Category;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.entity.ProductVariant;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.mapper.ProductMapper;
import com.rajitha.ecommerce.repository.ProductRepository;
import com.rajitha.ecommerce.repository.ProductVariantRepository;
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

        Mockito.when(productMapper.toProductEntity(dto)).thenReturn(product);
        Mockito.when(productRepository.save(Mockito.any(Product.class))).thenReturn(product);

        Integer id = productServiceIMPL.createProduct(dto);

        assertNotNull(id);
        assertEquals(1, id);

        Mockito.verify(productMapper,Mockito.times(1)).toProductEntity(dto);

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

        ProductPurchaseResponseDTO dto1 = new ProductPurchaseResponseDTO(1,1,"name1","description1","M","Red", new BigDecimal(265) , 63.0);
        ProductPurchaseResponseDTO dto2 = new ProductPurchaseResponseDTO(2,2,"name2","description2","L","Blue", new BigDecimal(262) , 54.0);




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
}
