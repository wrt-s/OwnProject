package com.wrt.excel.system.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO配置类
 */
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioConfig {
    
    /**
     * MinIO服务器地址
     */
    private String endpoint;
    
    /**
     * 访问密钥
     */
    private String accessKey;
    
    /**
     * 秘密密钥
     */
    private String secretKey;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 创建MinioClient Bean
     */
    @Bean
    public MinioClient minioClient() {
        try {
            if (endpoint == null || endpoint.isBlank()) {
                return null;
            }
            return MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        } catch (Exception e) {
            // MinIO optional — local upload fallback will be used
            return null;
        }
    }
}
