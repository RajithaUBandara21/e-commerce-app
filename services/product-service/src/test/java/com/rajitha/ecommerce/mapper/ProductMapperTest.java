package com.rajitha.ecommerce.mapper;

import com.rajitha.ecommerce.dto.ProductPurchaseResponseDTO;
import com.rajitha.ecommerce.dto.ProductRequestDTO;
import com.rajitha.ecommerce.dto.ProductResponseDTO;
import com.rajitha.ecommerce.dto.ProductVariantRequestDTO;
import com.rajitha.ecommerce.entity.Category;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.entity.ProductVariant;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

class ProductMapperTest {

   private ProductMapper productMapper;

   @BeforeEach
    void setUp() {
       productMapper = new ProductMapper();
   }
   @Test
    public void shouldMapToProductEntity(){
       ProductVariantRequestDTO variantDTO = ProductVariantRequestDTO.builder()
               .id(1)
               .sku("SHIRT-RED-M")
               .size("M")
               .color("Red")
               .availableQuantity(23)
               .build();

       ProductRequestDTO productRequestDTO = new ProductRequestDTO(
               1,
               "Rajitha Bandara",
               "test discription",
               new BigDecimal("256.26"),
               1,
               List.of(variantDTO)
       );

       Product product = productMapper.toProductEntity(productRequestDTO, "seller-1");
       Assertions.assertNotNull(product);
       // id is deliberately left null here — Hibernate's IDENTITY strategy needs a null id to
       // treat this as a new row to INSERT; a non-null id (even 0) makes save() try to UPDATE
       // a row that doesn't exist and fail. See ProductServiceIMPLTest for the regression case.
       Assertions.assertNull(product.getId());
       Assertions.assertEquals("seller-1", product.getSellerId());
       Assertions.assertEquals(product.getName(), productRequestDTO.name());
       Assertions.assertEquals(product.getDescription(), productRequestDTO.description());
       Assertions.assertEquals(product.getPrice(), productRequestDTO.price());
       Assertions.assertEquals(1, product.getVariants().size());
       Assertions.assertEquals(variantDTO.sku(), product.getVariants().get(0).getSku());
       Assertions.assertEquals(variantDTO.size(), product.getVariants().get(0).getSize());
       Assertions.assertEquals(variantDTO.color(), product.getVariants().get(0).getColor());
       Assertions.assertEquals(variantDTO.availableQuantity(), product.getVariants().get(0).getAvailableQuantity());
       Assertions.assertEquals(product, product.getVariants().get(0).getProduct());
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
       product.setPrice(new BigDecimal("999.99"));
       product.setCategory(category);

       ProductVariant variant = ProductVariant.builder()
               .id(1)
               .sku("IP15-BLK-128")
               .size("128GB")
               .color("Black")
               .availableQuantity(10)
               .product(product)
               .build();
       product.setVariants(List.of(variant));

       category.getProducts().add(product);

       ProductResponseDTO productResponseDTO = productMapper.toProductResponseDTO(product,
               List.of("http://localhost:9000/product-images/products/1/a.jpg"),
               new com.rajitha.ecommerce.dto.RatingSummaryDTO(4.5, 2));
       Assertions.assertNotNull(productResponseDTO);
       Assertions.assertEquals(product.getId(), productResponseDTO.id());
       Assertions.assertEquals(product.getName(), productResponseDTO.name());
       Assertions.assertEquals(product.getDescription(), productResponseDTO.description());
       Assertions.assertEquals(product.getPrice(), productResponseDTO.price());
       Assertions.assertEquals(product.getCategory().getId(), productResponseDTO.categoryId());
       Assertions.assertEquals(product.getCategory().getName(), productResponseDTO.categoryName());
       Assertions.assertEquals(product.getCategory().getDescription(), productResponseDTO.categoryDescription());
       Assertions.assertEquals(1, productResponseDTO.imageUrls().size());
       Assertions.assertEquals(1, productResponseDTO.variants().size());
       Assertions.assertEquals(variant.getSku(), productResponseDTO.variants().get(0).sku());
       Assertions.assertEquals(variant.getAvailableQuantity(), productResponseDTO.variants().get(0).availableQuantity());
       Assertions.assertEquals(4.5, productResponseDTO.averageRating());
       Assertions.assertEquals(2, productResponseDTO.reviewCount());

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
       product.setPrice(new BigDecimal("999.99"));
       product.setSellerId("seller-1");
       product.setCategory(category);

       category.getProducts().add(product);

       ProductVariant variant = ProductVariant.builder()
               .id(1)
               .sku("IP15-BLK-128")
               .size("128GB")
               .color("Black")
               .availableQuantity(10)
               .product(product)
               .build();

       double productQuantity = 5;

       ProductPurchaseResponseDTO productPurchaseResponseDTO = productMapper.toProductPurchaseResponseDTO(variant, productQuantity);

       Assertions.assertNotNull(productPurchaseResponseDTO);
       Assertions.assertEquals(variant.getId() , productPurchaseResponseDTO.variantId());
       Assertions.assertEquals(product.getId() , productPurchaseResponseDTO.productId());
       Assertions.assertEquals(product.getPrice() , productPurchaseResponseDTO.price());
       Assertions.assertEquals(product.getName() , productPurchaseResponseDTO.name());
       Assertions.assertEquals(product.getDescription() , productPurchaseResponseDTO.description());
       Assertions.assertEquals(variant.getSize() , productPurchaseResponseDTO.size());
       Assertions.assertEquals(variant.getColor() , productPurchaseResponseDTO.color());
       Assertions.assertEquals(productQuantity , productPurchaseResponseDTO.quantity());
       Assertions.assertEquals("seller-1" , productPurchaseResponseDTO.sellerId());


   }



//Null handel

   @Test
   public void shouldThrowNullPointExceptionMapToProductEntity(){

      var message = Assertions.assertThrows(NullPointerException.class, () -> productMapper.toProductEntity(null, "seller-1"));
      Assertions.assertEquals(message.getMessage(), "productRequestDTO is null");
   }

   @Test
   public void shouldThrowNullProductExceptionMapToProductResponseDTO(){
      var message = Assertions.assertThrows(NullPointerException.class, () -> productMapper.toProductResponseDTO(null, List.of(), null));
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
      product.setPrice(new BigDecimal("999.99"));
      product.setCategory(category);

      category.getProducts().add(product);

      ProductVariant variant = ProductVariant.builder()
              .id(1)
              .sku("IP15-BLK-128")
              .size("128GB")
              .color("Black")
              .availableQuantity(10)
              .product(product)
              .build();

   var nullProductMessage = Assertions.assertThrows(NullPointerException.class, () -> productMapper.toProductPurchaseResponseDTO(null , 1));
   var minusValueProductQuantityMessage = Assertions.assertThrows(IllegalArgumentException.class, ()-> productMapper.toProductPurchaseResponseDTO(variant , -20));
   Assertions.assertEquals(nullProductMessage.getMessage(), "ProductVariant is null");
   Assertions.assertEquals(minusValueProductQuantityMessage.getMessage() , "Product quantity must be greater than 0");
   }

}
