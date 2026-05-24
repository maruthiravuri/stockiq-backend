package com.stockiq.market.client;

import com.stockiq.market.dto.QuoteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Facade over all market data sources.
 * Priority chain (first configured source wins):
 *   1. Alpaca  — real-time IEX feed, free with paper account, batch-efficient
 *   2. Finnhub — 15-min delayed, 60 calls/min free
 *   3. Alpha Vantage — 15-min delayed, 25 calls/day free
 *   4. Mock    — always works, realistic random data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataClient {

    private final AlpacaClient       alpacaClient;
    private final FinnhubClient      finnhubClient;
    private final AlphaVantageClient alphaVantageClient;

    @Cacheable(value = "quotes", key = "#symbol")
    public QuoteDto getQuote(String symbol) {

        // 1. Alpaca — real-time
        if (alpacaClient.isConfigured()) {
            try {
                QuoteDto q = alpacaClient.getQuote(symbol);
                log.debug("✅ Alpaca [LIVE] {}: ${}", symbol, q.price());
                return q;
            } catch (Exception e) {
                log.warn("Alpaca failed for {}: {} — trying Finnhub", symbol, e.getMessage());
            }
        }

        // 2. Finnhub — 15-min delayed
        if (finnhubClient.isConfigured()) {
            try {
                QuoteDto q = finnhubClient.getQuote(symbol);
                log.debug("✅ Finnhub [15min] {}: ${}", symbol, q.price());
                return q;
            } catch (Exception e) {
                log.warn("Finnhub failed for {}: {} — trying Alpha Vantage", symbol, e.getMessage());
            }
        }

        // 3. Alpha Vantage / mock fallback
        return alphaVantageClient.getQuote(symbol);
    }

    /**
     * Batch fetch — Alpaca handles up to 100 symbols in ONE API call.
     * Falls back to individual calls for other providers.
     */
    public List<QuoteDto> getBatchQuotes(List<String> symbols) {
        if (alpacaClient.isConfigured()) {
            try {
                List<QuoteDto> quotes = alpacaClient.getBatchQuotes(symbols);
                if (!quotes.isEmpty()) {
                    log.debug("✅ Alpaca batch [LIVE]: {} symbols", quotes.size());
                    return quotes;
                }
            } catch (Exception e) {
                log.warn("Alpaca batch failed: {} — falling back", e.getMessage());
            }
        }

        // Sequential fallback for other providers
        return symbols.parallelStream()
            .map(this::getQuote)
            .collect(Collectors.toList());
    }
}
