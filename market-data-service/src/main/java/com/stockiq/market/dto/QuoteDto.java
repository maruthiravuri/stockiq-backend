package com.stockiq.market.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuoteDto(
    String symbol,
    String name,
    BigDecimal price,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal previousClose,
    Long volume,
    Long marketCap,
    BigDecimal peRatio,
    BigDecimal weekHigh52,
    BigDecimal weekLow52,
    String currency,
    String exchange,
    Instant timestamp
) {}
