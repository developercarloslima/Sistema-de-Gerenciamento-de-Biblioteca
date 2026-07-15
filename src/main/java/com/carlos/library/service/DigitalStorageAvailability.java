package com.carlos.library.service;

import com.carlos.library.config.StorageProperties;
import com.carlos.library.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DigitalStorageAvailability {
    private final StorageProperties properties;
    private final ObjectProvider<R2StorageService> storageProvider;

    public R2StorageService optional() {
        return properties.enabled() ? storageProvider.getIfAvailable() : null;
    }

    public R2StorageService required() {
        if (!properties.enabled()) {
            throw new BusinessException("O armazenamento de livros digitais ainda não foi configurado.");
        }
        R2StorageService storage = storageProvider.getIfAvailable();
        if (storage == null) throw new BusinessException("O armazenamento digital está indisponível.");
        return storage;
    }
}
