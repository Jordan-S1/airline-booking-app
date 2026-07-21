package com.airlinebookingsystem.dto.currency;

import java.math.BigDecimal;

/**
 * A supported display currency. All prices are stored in EUR (the base);
 * {@code rateFromEur} multiplies a EUR amount to convert it to this currency.
 */
public record CurrencyResponse(
        String code,
        String name,
        String symbol,
        BigDecimal rateFromEur
) {}
