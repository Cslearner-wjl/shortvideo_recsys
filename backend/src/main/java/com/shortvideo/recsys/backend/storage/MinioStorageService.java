package com.shortvideo.recsys.backend.storage;

import com.shortvideo.recsys.backend.common.BizException;
import com.shortvideo.recsys.backend.common.ErrorCodes;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@Profile({"docker", "test"})
public class MinioStorageService {
    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            }
        } catch (Exception e) {
            throw new BizException(ErrorCodes.MINIO_UNAVAILABLE, "对象存储不可用");
        }
    }

    public void putObject(String objectKey, String contentType, InputStream inputStream, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .contentType(Objects.requireNonNullElse(contentType, MediaType.APPLICATION_OCTET_STREAM_VALUE))
                            .stream(inputStream, size, -1)
                            .build()
            );
        } catch (Exception e) {
            throw new BizException(ErrorCodes.MINIO_UNAVAILABLE, "对象存储不可用");
        }
    }

    public void removeObject(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(properties.getBucket()).object(objectKey).build());
        } catch (Exception e) {
            if (e instanceof ErrorResponseException ere && ere.errorResponse() != null) {
                String code = ere.errorResponse().code();
                if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code)) {
                    return;
                }
            }
            throw new BizException(ErrorCodes.MINIO_UNAVAILABLE, "对象存储不可用");
        }
    }

    public StatObjectResponse statObject(String objectKey) {
        try {
            return minioClient.statObject(StatObjectArgs.builder().bucket(properties.getBucket()).object(objectKey).build());
        } catch (Exception e) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "资源不存在");
        }
    }

    public String toPublicUrl(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        String base = Objects.requireNonNullElse(properties.getPublicEndpoint(), properties.getEndpoint());
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        URI uri = URI.create(normalized);
        return uri.toString() + "/" + properties.getBucket() + "/" + objectKey;
    }
}
