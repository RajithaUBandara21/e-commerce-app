package com.rajitha.ecommerce.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties properties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    // Product images are public storefront content, not user data — the bucket is
    // deliberately world-readable (GetObject only) so product cards/PDPs can load
    // images directly from MinIO without a presigned GET per view.
    @Bean
    public ApplicationRunner ensureProductImageBucket(MinioClient minioClient) {
        return (ApplicationArguments args) -> {
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
                    log.info("Created MinIO bucket '{}'", properties.bucket());
                }
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(properties.bucket())
                        .config(publicReadPolicy(properties.bucket()))
                        .build());
            } catch (Exception e) {
                log.warn("Could not initialize MinIO bucket '{}' — is MinIO running? Image upload/read will fail until it is: {}",
                        properties.bucket(), e.getMessage());
            }
        };
    }

    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }
}
