package com.wrt.excel.system.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * File upload: MinIO stores object key (not expired presigned URL);
 * local uploads use /uploads/{name}. List APIs call {@link #toReadableUrl(String)}.
 */
@Slf4j
@Service
public class FileUploadService {

    public static final String MINIO_KEY_PREFIX = "minio:";
    private static final int PRESIGN_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    @Autowired(required = false)
    private MinioClient minioClient;

    @Autowired(required = false)
    private com.wrt.excel.system.config.MinioConfig minioConfig;

    @Value("${love.upload.dir:uploads}")
    private String uploadDir;

    /** Public URL only when explicitly enabled. Default refreshes a presigned GET on each read. */
    @Value("${minio.public-read:false}")
    private boolean publicRead;

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只支持图片格式");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        if (minioClient != null && minioConfig != null
                && minioConfig.getEndpoint() != null
                && !minioConfig.getEndpoint().isBlank()) {
            try {
                return uploadToMinio(file, fileName, contentType);
            } catch (Exception e) {
                log.warn("MinIO upload failed, fallback to local: {}", e.getMessage());
            }
        } else {
            log.info("MinIO not configured, using local upload");
        }
        return uploadToLocal(file, fileName);
    }

    private String uploadToMinio(MultipartFile file, String fileName, String contentType) throws Exception {
        ensureBucketExists();
        trySetPublicReadPolicy();
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        }
        // Persist object key (or permanent public URL), NEVER a short-lived presigned URL
        if (publicRead) {
            String permanent = buildPublicUrl(fileName);
            log.info("Image uploaded (MinIO public): {}", permanent);
            return permanent;
        }
        String key = MINIO_KEY_PREFIX + fileName;
        log.info("Image uploaded (MinIO key): {}", key);
        return key;
    }

    private String buildPublicUrl(String objectName) {
        String endpoint = minioConfig.getEndpoint().replaceAll("/+$", "");
        return endpoint + "/" + minioConfig.getBucketName() + "/" + objectName;
    }

    private void trySetPublicReadPolicy() {
        if (!publicRead || minioClient == null || minioConfig == null) return;
        try {
            String bucket = minioConfig.getBucketName();
            String policy = """
                    {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},"Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}
                    """.formatted(bucket).trim();
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build()
            );
        } catch (Exception e) {
            log.warn("Set MinIO public-read policy failed (will use refreshable URLs): {}", e.getMessage());
            publicRead = false;
        }
    }

    private String uploadToLocal(MultipartFile file, String fileName) {
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String url = "/uploads/" + fileName;
            log.info("Image uploaded (local): {} -> {}", fileName, target);
            return url;
        } catch (Exception e) {
            log.error("Local image upload failed", e);
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );
            if (!found) {
                minioClient.makeBucket(
                        io.minio.MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("确保存储桶失败: " + e.getMessage(), e);
        }
    }

    /**
     * Convert stored imageUrl (object key / expired presign / public / local) to a currently readable URL.
     */
    public String toReadableUrl(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        String s = stored.trim();
        if (s.startsWith("/uploads/") || s.startsWith("data:")) return s;

        String object = extractObjectKey(s);
        if (object == null || object.isBlank()) return s;

        // Expired presigned URLs 403 even if a public-read flag is on; always re-sign those.
        boolean expiredPresign = s.contains("X-Amz-Signature") || s.contains("X-Amz-Algorithm")
                || s.startsWith(MINIO_KEY_PREFIX);
        if (!expiredPresign && publicRead && minioConfig != null && minioConfig.getEndpoint() != null
                && !minioConfig.getEndpoint().isBlank()) {
            return buildPublicUrl(object);
        }
        return presign(object);
    }

    public String extractObjectKey(String stored) {
        if (stored == null || stored.isBlank()) return null;
        String s = stored.trim();
        if (s.startsWith(MINIO_KEY_PREFIX)) {
            return s.substring(MINIO_KEY_PREFIX.length());
        }
        if (s.startsWith("/uploads/")) {
            return null; // local path, not MinIO
        }
        // Bare object name
        if (!s.contains("://") && !s.contains("?") && !s.contains("/")) {
            return s;
        }
        try {
            // Strip query (presigned params) and take last path segment
            String noQuery = s.contains("?") ? s.substring(0, s.indexOf('?')) : s;
            URI uri = URI.create(noQuery);
            String path = uri.getPath();
            if (path == null || path.isBlank()) return null;
            String[] parts = path.split("/");
            String last = parts[parts.length - 1];
            if (last == null || last.isBlank()) return null;
            // Skip if last segment is bucket name only
            if (minioConfig != null && last.equals(minioConfig.getBucketName())) return null;
            return last;
        } catch (Exception e) {
            int slash = s.lastIndexOf('/');
            String last = slash >= 0 ? s.substring(slash + 1) : s;
            int q = last.indexOf('?');
            return q >= 0 ? last.substring(0, q) : last;
        }
    }

    private String presign(String objectName) {
        if (minioClient == null || minioConfig == null || objectName == null || objectName.isBlank()) {
            return objectName;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .expiry(PRESIGN_EXPIRY_SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Presign failed for {}: {}", objectName, e.getMessage());
            if (minioConfig.getEndpoint() != null) {
                return buildPublicUrl(objectName);
            }
            return objectName;
        }
    }
}
