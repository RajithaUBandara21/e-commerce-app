package com.rajitha.ecommerce.client.rest;

// Import DTOs (data transfer objects)
import com.rajitha.ecommerce.dto.PurchaseRequestDTO;
import com.rajitha.ecommerce.dto.PurchaseResponseDTO;

// Custom exception for business-level errors
import com.rajitha.ecommerce.exception.BusinessException;

// Lombok annotation to generate constructor for final fields
import lombok.RequiredArgsConstructor;

// Spring annotations and classes
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;


import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@Component


@RequiredArgsConstructor
public class ProductClient {


    @Value("${application.config.product-url}")
    private String productUrl;


    private final RestTemplate restTemplate;

    public List<PurchaseResponseDTO> purchaseProducts(List<PurchaseRequestDTO> requestBody) {


        HttpHeaders headers = new HttpHeaders();


        headers.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);

        // Create HTTP request entity (body + headers)
        HttpEntity<List<PurchaseRequestDTO>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Define expected response type (List<PurchaseResponseDTO>)
        // Needed because of Java generics type erasure
        ParameterizedTypeReference<List<PurchaseResponseDTO>> responseType =
                new ParameterizedTypeReference<>() {};

        // Make HTTP POST request to product service
        ResponseEntity<List<PurchaseResponseDTO>> responseEntity =
                restTemplate.exchange(
                        productUrl + "/purchase",   // URL endpoint
                        HttpMethod.POST,           // HTTP method
                        requestEntity,             // Request body + headers
                        responseType               // Expected response type
                );

        // Check if response status is error (4xx or 5xx)
        if (responseEntity.getStatusCode().isError()) {

            // Throw custom business exception if error occurs
            throw new BusinessException(
                    "An error occurred while processing the products purchases :: status code "
                            + responseEntity.getStatusCode()
            );
        }

        // Return response body (list of purchase results)
        return responseEntity.getBody();
    }
}