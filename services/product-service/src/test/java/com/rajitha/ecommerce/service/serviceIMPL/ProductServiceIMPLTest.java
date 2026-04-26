package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.entity.Category;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.exeption.ProductPurchaseException;
import com.rajitha.ecommerce.mapper.ProductMapper;
import com.rajitha.ecommerce.repository.ProductRepository;
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

    @Test
    void shouldSuccessfullyCreateProduct() {

        ProductRequestDTO dto = ProductRequestDTO.builder()
                .id(1)
                .name("Test name")
                .description("Test description")
                .availableQuantity(1256)
                .price(new BigDecimal("256849"))
                .categoryId(256)
                .build();

        Product product = Product.builder()
                .id(1)
                .name("Test name")
                .description("Test description")
                .availableQuantity(1256)
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

        Product product1 = new Product(1,"name1","description1", 65,new BigDecimal(265),Category.builder().id(1).name("categoryName1").build() );
        Product product2 = new Product(2,"name2","description2", 55,new BigDecimal(262),Category.builder().id(2).name("categoryName2").build() );

        List<Product> sortedProduct = List.of(
                product1,
                product2
        );

        ProductPurchaseResponseDTO dto1 =  new  ProductPurchaseResponseDTO(1,"name1","description1", new BigDecimal(265) , 63.0);
        ProductPurchaseResponseDTO dto2 = new  ProductPurchaseResponseDTO(2,"name2","description2", new BigDecimal(262) , 54.0);





        Mockito.when(productRepository.findAllByIdInOrderById(Mockito.anyList())).thenReturn(sortedProduct);

        Mockito.when(productMapper.toProductPurchaseResponseDTO(Mockito.eq(product1),Mockito.anyDouble())).thenReturn( dto1);
        Mockito.when(productMapper.toProductPurchaseResponseDTO(Mockito.eq(product2),Mockito.anyDouble())).thenReturn( dto2);

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

        Product product1 = new Product(1,"name1","description1", 65,new BigDecimal(265),Category.builder().id(1).name("categoryName1").build() );


        List<Product> sortedProduct = List.of(
                product1
        );


        Mockito.when(productRepository.findAllByIdInOrderById(Mockito.anyList())).thenReturn(sortedProduct);



var productNotExistException = Assertions.assertThrows(
                ProductPurchaseException.class,
                () -> productServiceIMPL.purchaseProductService(requests1)
        );

        Assertions.assertEquals("One or more product not exists",productNotExistException.getMessage());

        var productRequestCountMinusException = Assertions.assertThrows(
                ProductPurchaseException.class,
                () -> productServiceIMPL.purchaseProductService(requests2)
        );

        Assertions.assertEquals( "Insufficient stock quantity for product with id"+requests2.get(0).productId() ,productRequestCountMinusException.getMessage());
    }
}