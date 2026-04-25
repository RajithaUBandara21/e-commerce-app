package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.entity.Category;
import com.rajitha.ecommerce.entity.Product;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.ArrayList;

class ProductMapperTest {

   private ProductMapper productMapper;

   @BeforeEach
    void setUp() {
       productMapper = new ProductMapper();
   }
   @Test
    public void shouldMapToProductEntity(){
       ProductRequestDTO productRequestDTO = new ProductRequestDTO(
               1,
               "Rajitha Bandara",
               "test discription",
               23,
               new BigDecimal("256.26"),
               1
       );

       Product product = productMapper.toProductEntity(productRequestDTO);
       Assertions.assertNotNull(product);
       Assertions.assertEquals(product.getId(), productRequestDTO.id());
       Assertions.assertEquals(product.getName(), productRequestDTO.name());
       Assertions.assertEquals(product.getDescription(), productRequestDTO.description());
       Assertions.assertEquals(product.getPrice(), productRequestDTO.price());
       Assertions.assertEquals(product.getAvailableQuantity() , productRequestDTO.availableQuantity());
       Assertions.assertEquals(product.getPrice(), productRequestDTO.price());
   }


   @Test
    public void shouldMapToProductResponseDTO(){

       Category category = new Category();
       category.setId(1);
       category.setName("Electronics");
       category.setDescription("Electronic items");
       category.setProducts(new ArrayList<>());

       Product product = new Product();
       product.setId(1);
       product.setName("iPhone 15");
       product.setDescription("Apple phone");
       product.setAvailableQuantity(10);
       product.setPrice(new BigDecimal("999.99"));
       product.setCategory(category);

       category.getProducts().add(product);

       ProductResponseDTO productResponseDTO = productMapper.toProductResponseDTO(product);
       Assertions.assertNotNull(productResponseDTO);
       Assertions.assertEquals(product.getId(), productResponseDTO.id());
       Assertions.assertEquals(product.getName(), productResponseDTO.name());
       Assertions.assertEquals(product.getDescription(), productResponseDTO.description());
       Assertions.assertEquals(product.getPrice(), productResponseDTO.price());
       Assertions.assertEquals(product.getAvailableQuantity() , productResponseDTO.availableQuantity());
       Assertions.assertEquals(product.getCategory().getId(), productResponseDTO.categoryId());
       Assertions.assertEquals(product.getCategory().getName(), productResponseDTO.categoryName());
       Assertions.assertEquals(product.getCategory().getDescription(), productResponseDTO.categoryDescription());

   }



   @Test
    public void shouldMapToProductPurchaseResponseDTO(){

       Category category = new Category();
       category.setId(1);
       category.setName("Electronics");
       category.setDescription("Electronic items");
       category.setProducts(new ArrayList<>());

       Product product = new Product();
       product.setId(1);
       product.setName("iPhone 15");
       product.setDescription("Apple phone");
       product.setAvailableQuantity(10);
       product.setPrice(new BigDecimal("999.99"));
       product.setCategory(category);

       category.getProducts().add(product);

       double productQuantity = 5;

       ProductPurchaseResponseDTO productPurchaseResponseDTO = productMapper.toProductPurchaseResponseDTO(product, productQuantity);

       Assertions.assertNotNull(productPurchaseResponseDTO);
       Assertions.assertEquals(product.getId() , productPurchaseResponseDTO.productId());
       Assertions.assertEquals(product.getPrice() , productPurchaseResponseDTO.price());
       Assertions.assertEquals(product.getName() , productPurchaseResponseDTO.name());
       Assertions.assertEquals(product.getDescription() , productPurchaseResponseDTO.description());
       Assertions.assertEquals(productQuantity , productPurchaseResponseDTO.quantity());


   }



//Null handel

   @Test
   public void shouldThrowNullPointExceptionMapToProductEntity(){

      var message = Assertions.assertThrows(NullPointerException.class, () -> productMapper.toProductEntity(null));
      Assertions.assertEquals(message.getMessage(), "productRequestDTO is null");
   }

   @Test
   public void shouldThrowNullProductExceptionMapToProductResponseDTO(){
      var message = Assertions.assertThrows(NullPointerException.class, () -> productMapper.toProductResponseDTO(null));
      Assertions.assertEquals(message.getMessage(), "Product is null");   }

   @Test
   public void shouldThrowNullExceptionMapToProductPurchaseResponseDTO(){
      Category category = new Category();
      category.setId(1);
      category.setName("Electronics");
      category.setDescription("Electronic items");
      category.setProducts(new ArrayList<>());

      Product product = new Product();
      product.setId(1);
      product.setName("iPhone 15");
      product.setDescription("Apple phone");
      product.setAvailableQuantity(10);
      product.setPrice(new BigDecimal("999.99"));
      product.setCategory(category);

      category.getProducts().add(product);

   var nullProductMessage = Assertions.assertThrows(NullPointerException.class, () -> productMapper.toProductPurchaseResponseDTO(null , 1));
   var minusValueProductQuantityMessage = Assertions.assertThrows(IllegalArgumentException.class, ()-> productMapper.toProductPurchaseResponseDTO(product , -20));
   Assertions.assertEquals(nullProductMessage.getMessage(), "Product is null");
   Assertions.assertEquals(minusValueProductQuantityMessage.getMessage() , "Product quantity must be greater than 0");
   }

}