package com.rajitha.ecommerce.service.serviceIMPL;

import com.rajitha.ecommerce.config.MinioProperties;
import com.rajitha.ecommerce.entity.Product;
import com.rajitha.ecommerce.entity.ProductImage;
import com.rajitha.ecommerce.exeption.ProductAccessDeniedException;
import com.rajitha.ecommerce.repository.ProductImageRepository;
import com.rajitha.ecommerce.repository.ProductRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceIMPLTest {

    @InjectMocks
    private ProductImageServiceIMPL productImageService;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private MinioClient minioClient;
    @Mock
    private MinioProperties minioProperties;

    @Test
    void shouldGenerateUploadUrlForOwningSeller() throws Exception {
        var product = Product.builder().id(1).sellerId("seller-1").build();
        Mockito.when(productRepository.findById(1)).thenReturn(Optional.of(product));
        Mockito.when(minioProperties.bucket()).thenReturn("product-images");
        Mockito.when(minioClient.getPresignedObjectUrl(Mockito.any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio.local/presigned-put");

        var response = productImageService.generateUploadUrl(1, "image/png", "seller-1", false);

        Assertions.assertEquals("https://minio.local/presigned-put", response.uploadUrl());
        Assertions.assertTrue(response.objectKey().startsWith("products/1/"));
        Assertions.assertTrue(response.objectKey().endsWith(".png"));
    }

    @Test
    void shouldRejectUploadUrlForNonOwningSeller() {
        var product = Product.builder().id(1).sellerId("seller-1").build();
        Mockito.when(productRepository.findById(1)).thenReturn(Optional.of(product));

        Assertions.assertThrows(ProductAccessDeniedException.class,
                () -> productImageService.generateUploadUrl(1, "image/png", "seller-2", false));
    }

    @Test
    void shouldRegisterImageAtNextPosition() {
        var product = Product.builder().id(1).sellerId("seller-1").build();
        Mockito.when(productRepository.findById(1)).thenReturn(Optional.of(product));
        Mockito.when(productImageRepository.countByProduct_Id(1)).thenReturn(2);
        Mockito.when(minioProperties.bucket()).thenReturn("product-images");
        Mockito.when(minioProperties.publicBaseUrl()).thenReturn("http://localhost:9000");
        Mockito.when(productImageRepository.save(Mockito.any(ProductImage.class))).thenAnswer(invocation -> {
            ProductImage img = invocation.getArgument(0);
            img.setId(5);
            return img;
        });

        var response = productImageService.registerImage(1, "products/1/photo.jpg", "seller-1", false);

        Assertions.assertEquals(2, response.position());
        Assertions.assertEquals("http://localhost:9000/product-images/products/1/photo.jpg", response.url());
    }

    @Test
    void shouldThrowWhenDeletingImageBelongingToAnotherProduct() {
        var product = Product.builder().id(1).sellerId("seller-1").build();
        var otherProduct = Product.builder().id(2).sellerId("seller-1").build();
        var image = ProductImage.builder().id(9).product(otherProduct).objectKey("products/2/x.jpg").build();

        Mockito.when(productRepository.findById(1)).thenReturn(Optional.of(product));
        Mockito.when(productImageRepository.findById(9)).thenReturn(Optional.of(image));

        Assertions.assertThrows(EntityNotFoundException.class,
                () -> productImageService.deleteImage(1, 9, "seller-1", false));
    }

    @Test
    void shouldFindImageUrlsOrderedByPosition() {
        var product = Product.builder().id(1).build();
        var image1 = ProductImage.builder().id(1).product(product).objectKey("a.jpg").position(0).build();
        var image2 = ProductImage.builder().id(2).product(product).objectKey("b.jpg").position(1).build();

        Mockito.when(productImageRepository.findByProduct_IdOrderByPosition(1)).thenReturn(List.of(image1, image2));
        Mockito.when(minioProperties.bucket()).thenReturn("product-images");
        Mockito.when(minioProperties.publicBaseUrl()).thenReturn("http://localhost:9000");

        var urls = productImageService.findImageUrlsByProductId(1);

        Assertions.assertEquals(List.of(
                "http://localhost:9000/product-images/a.jpg",
                "http://localhost:9000/product-images/b.jpg"
        ), urls);
    }
}
