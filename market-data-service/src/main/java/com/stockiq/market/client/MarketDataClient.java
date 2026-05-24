package com.stockiq.market.client;

import com.stockiq.market.dto.QuoteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Facade that tries data sources in priority order:
 * 1. Finnhub (if FINNHUB_API_KEY configured) — 60 calls/min free
 * 2. Alpha Vantage (if ALPHA_VANTAGE_API_KEY != demo) — 25 calls/day free
 * 3. Mock data fallback
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataClient {

    private final FinnhubClient finnhubClient;
    private final AlphaVantageClient alphaVantageClient;

    @Cacheable(value = "quotes", key = "#symbol")
    public QuoteDto getQuote(String symbol) {
        // Try Finnhub first
        if (finnhubClient.isConfigured()) {
            try {
                QuoteDto quote = finnhubClient.getQuote(symbol);
                log.debug("Finnhub quote for {}: ${}", symbol, quote.price());
                return quote;
            } catch (Exception e) {
                log.warn("Finnhub failed for {}, trying Alpha Vantage: {}", symbol, e.getMessage());
            }
        }

        // Fall back to Alpha Vantage (includes its own mock fallback)
        return alphaVantageClient.getQuote(symbol);
    }
}
