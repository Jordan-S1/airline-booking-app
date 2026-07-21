package com.airlinebookingsystem.repository;

import com.airlinebookingsystem.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for supported display currencies and their EUR-based rates.
 */
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyCode(String currencyCode);

    List<ExchangeRate> findAllByOrderByCurrencyCode();

    boolean existsByCurrencyCode(String currencyCode);
}
