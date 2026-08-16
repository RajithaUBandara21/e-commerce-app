package com.rajitha.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        // What browsers actually load images from — same as endpoint in local dev,
        // but kept separate since a real deployment would front this with a CDN
        // domain rather than expose MinIO's own address publicly.
        String publicBaseUrl
) {
}
