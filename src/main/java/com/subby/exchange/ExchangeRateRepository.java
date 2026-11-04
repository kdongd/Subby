package com.subby.exchange;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, ExchangeRateId> {

    List<ExchangeRate> findAllByBaseAndQuote(String base, String quote);

    Optional<ExchangeRate> findTopByBaseAndQuoteOrderByRateDateDesc(String base, String quote);
}
