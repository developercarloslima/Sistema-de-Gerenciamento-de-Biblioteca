package com.carlos.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.library")
public record LibraryProperties(
        int defaultLoanDays,
        int maxRenewals,
        int reservationHoldHours,
        int dueSoonDays,
        BigDecimal dailyFine,
        BigDecimal maxUnpaidFine
) {}
