package com.airlinebookingsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A supported display currency and its conversion rate relative to the
 * base currency (EUR). All stored prices are in EUR; {@code rateFromEur}
 * multiplies a EUR amount to produce the amount in this currency.
 */
@Entity
@Table(name = "exchange_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 50)
    private String currencyName;

    @Column(nullable = false, length = 5)
    private String symbol;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal rateFromEur;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
