package com.carlos.library.service;

import com.carlos.library.config.StorageProperties;
import com.carlos.library.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage.r2", name = "enabled", havingValue = "true")
public class R2StorageService {
    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties properties;

    public PresignedUpload createUploadUrl(String objectKey, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest request = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(properties.uploadExpirationMinutes()))
                .putObjectRequest(objectRequest)
                .build());

        Map<String, String> headers = new LinkedHashMap<>();
        request.signedHeaders().forEach((key, values) -> {
            if (!values.isEmpty() && !key.equalsIgnoreCase("host")) headers.put(key, values.getFirst());
        });
        return new PresignedUpload(request.url().toString(), headers);
    }

    public HeadObjectResponse head(String objectKey) {
        try {
            return s3.headObject(HeadObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build());
        } catch (S3Exception ex) {
            throw new BusinessException("O arquivo enviado não foi encontrado no armazenamento.");
        }
    }

    public byte[] readPrefix(String objectKey, int lastByteInclusive) {
        ResponseBytes<GetObjectResponse> bytes = s3.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .range("bytes=0-" + lastByteInclusive)
                .build());
        return bytes.asByteArray();
    }

    public PresignedAccess createAccessUrl(String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(properties.accessExpirationMinutes()))
                .getObjectRequest(request)
                .build());
        return new PresignedAccess(presigned.url().toString(), properties.accessExpirationMinutes());
    }

    public void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build());
        } catch (RuntimeException ex) {
            log.warn("Não foi possível excluir o objeto antigo do R2: {}", objectKey, ex);
        }
    }


    public record PresignedUpload(String url, Map<String, String> headers) {}
    public record PresignedAccess(String url, int expiresInMinutes) {}
}
