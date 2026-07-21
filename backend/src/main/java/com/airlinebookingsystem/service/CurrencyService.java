package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.currency.CurrencyResponse;
import com.airlinebookingsystem.entity.ExchangeRate;
import com.airlinebookingsystem.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CurrencyService {

    public static final String BASE_CURRENCY = "EUR";

    private final ExchangeRateRepository exchangeRateRepository;

    public List<CurrencyResponse> getAllCurrencies() {
        log.info("Fetching all supported currencies");
        return exchangeRateRepository.findAllByOrderByCurrencyCode().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Validates that a currency code is supported. Used when persisting a
     * user's preferred currency so we never store an unknown code.
     */
    public boolean isSupported(String currencyCode) {
        if (currencyCode == null) return false;
        return exchangeRateRepository.existsByCurrencyCode(currencyCode.toUpperCase());
    }

    private CurrencyResponse mapToResponse(ExchangeRate rate) {
        return new CurrencyResponse(
                rate.getCurrencyCode(),
                rate.getCurrencyName(),
                rate.getSymbol(),
                rate.getRateFromEur()
        );
    }
}
