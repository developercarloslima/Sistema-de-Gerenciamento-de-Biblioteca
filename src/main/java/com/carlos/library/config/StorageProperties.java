package com.carlos.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.r2")
public record StorageProperties(
        boolean enabled,
        String endpoint,
        String accessKeyId,
        String secretAccessKey,
        String bucket,
        int uploadExpirationMinutes,
        int accessExpirationMinutes,
        long maxPdfBytes,
        long maxCoverBytes
) {
    public void validate() {
        if (!enabled) return;
        if (isBlank(endpoint) || isBlank(accessKeyId) || isBlank(secretAccessKey) || isBlank(bucket)) {
            throw new IllegalStateException("As configurações do Cloudflare R2 estão incompletas.");
        }
        if (uploadExpirationMinutes < 1 || accessExpirationMinutes < 1) {
            throw new IllegalStateException("Os prazos das URLs assinadas devem ser maiores que zero.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
