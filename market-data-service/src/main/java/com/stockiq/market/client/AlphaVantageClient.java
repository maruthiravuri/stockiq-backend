package com.stockiq.market.client;

import com.stockiq.market.dto.QuoteDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlphaVantageClient {

    private final RestClient restClient;

    @Value("${market.alpha-vantage.api-key:demo}")
    private String apiKey;

    @Value("${market.alpha-vantage.base-url:https://www.alphavantage.co}")
    private String baseUrl;

    @CircuitBreaker(name = "alphaVantage", fallbackMethod = "quoteFallback")
    @Retry(name = "alphaVantage")
    @Cacheable(value = "quotes", key = "#symbol", unless = "#result == null")
    public QuoteDto getQuote(String symbol) {
        log.debug("Fetching quote for {}", symbol);
        try {
            var response = restClient.get()
                    .uri(baseUrl + "/query?function=GLOBAL_QUOTE&symbol={s}&apikey={k}", symbol, apiKey)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("Global Quote")) {
                @SuppressWarnings("unchecked")
                Map<String, String> q = (Map<String, String>) response.get("Global Quote");
                return mapToQuoteDto(symbol, q);
            }
        } catch (Exception e) {
            log.warn("Alpha Vantage call failed for {}: {}", symbol, e.getMessage());
        }
        return quoteFallback(symbol, new RuntimeException("API unavailable"));
    }

    public QuoteDto quoteFallback(String symbol, Throwable t) {
        log.info("Using fallback mock data for symbol: {}", symbol);
        return generateMockQuote(symbol);
    }

    private QuoteDto mapToQuoteDto(String symbol, Map<String, String> q) {
        BigDecimal price = new BigDecimal(q.getOrDefault("05. price", "0"));
        BigDecimal change = new BigDecimal(q.getOrDefault("09. change", "0"));
        BigDecimal changePct = new BigDecimal(
                q.getOrDefault("10. change percent", "0%").replace("%", "")
        ).setScale(4, RoundingMode.HALF_UP);

        return new QuoteDto(
                symbol, symbol,
                price, change, changePct,
                new BigDecimal(q.getOrDefault("02. open", "0")),
                new BigDecimal(q.getOrDefault("03. high", "0")),
                new BigDecimal(q.getOrDefault("04. low", "0")),
                new BigDecimal(q.getOrDefault("08. previous close", "0")),
                Long.parseLong(q.getOrDefault("06. volume", "0")),
                null, null, null, null,
                "USD", "NASDAQ", Instant.now()
        );
    }

    private QuoteDto generateMockQuote(String symbol) {
        double base = switch (symbol) {
            case "AAPL" -> 189.84; case "MSFT" -> 374.51; case "NVDA" -> 495.22;
            case "GOOGL" -> 152.19; case "AMZN" -> 178.35; case "META" -> 481.73;
            case "TSLA" -> 248.42; case "GLD" -> 187.34; case "VOO" -> 468.23;
            default -> 100.0 + ThreadLocalRandom.current().nextDouble(50);
        };
        double delta = (ThreadLocalRandom.current().nextDouble() - 0.48) * base * 0.02;
        BigDecimal price = BigDecimal.valueOf(base + delta).setScale(2, RoundingMode.HALF_UP);
        BigDecimal change = BigDecimal.valueOf(delta).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pct = change.divide(price, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return new QuoteDto(symbol, symbol, price, change, pct,
                price, price, price, price,
                (long)(ThreadLocalRandom.current().nextDouble() * 50_000_000),
                null, null, null, null, "USD", "NASDAQ", Instant.now());
    }
}
