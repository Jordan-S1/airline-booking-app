package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.currency.CurrencyResponse;
import com.airlinebookingsystem.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Currencies", description = "Supported display currencies and EUR-based rates")
public class CurrencyController {

    private final CurrencyService currencyService;

    @Operation(summary = "List supported currencies",
            description = "Returns all supported display currencies with their rate relative to EUR (the base currency).")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> getAllCurrencies() {
        log.info("GET /currencies");
        return ResponseEntity.ok(currencyService.getAllCurrencies());
    }
}
