package com.carlos.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.storage.r2")
public record StorageProperties(
        @DefaultValue("false")
        boolean enabled,

        @DefaultValue("")
        String endpoint,

        @DefaultValue("auto")
        String region,

        @DefaultValue("")
        String accessKeyId,

        @DefaultValue("")
        String secretAccessKey,

        @DefaultValue("")
        String bucket,

        @DefaultValue("15")
        int uploadExpirationMinutes,

        @DefaultValue("60")
        int accessExpirationMinutes,

        @DefaultValue("52428800")
        long maxPdfBytes,

        @DefaultValue("5242880")
        long maxCoverBytes
) {

    public void validate() {
        if (!enabled) {
            return;
        }

        requireText(endpoint, "endpoint");
        requireText(region, "region");
        requireText(accessKeyId, "access-key-id");
        requireText(secretAccessKey, "secret-access-key");
        requireText(bucket, "bucket");

        if (uploadExpirationMinutes <= 0) {
            throw new IllegalStateException(
                    "app.storage.r2.upload-expiration-minutes deve ser maior que zero."
            );
        }

        if (accessExpirationMinutes <= 0) {
            throw new IllegalStateException(
                    "app.storage.r2.access-expiration-minutes deve ser maior que zero."
            );
        }

        if (maxPdfBytes <= 0) {
            throw new IllegalStateException(
                    "app.storage.r2.max-pdf-bytes deve ser maior que zero."
            );
        }

        if (maxCoverBytes <= 0) {
            throw new IllegalStateException(
                    "app.storage.r2.max-cover-bytes deve ser maior que zero."
            );
        }
    }

    private static void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "A propriedade app.storage.r2." + propertyName
                            + " é obrigatória quando o armazenamento está habilitado."
            );
        }
    }
}
