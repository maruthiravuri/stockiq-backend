package com.stockiq.market.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockiq.market.dto.QuoteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinnhubClient {

    private final RestClient restClient;

    @Value("${market.finnhub.api-key:#{null}}")
    private String apiKey;

    // Finnhub quote response shape
    @JsonIgnoreProperties(ignoreUnknown = true)
    record FinnhubQuote(
        @JsonProperty("c") double current,       // current price
        @JsonProperty("d") double change,        // change
        @JsonProperty("dp") double changePercent,// change percent
        @JsonProperty("h") double high,
        @JsonProperty("l") double low,
        @JsonProperty("o") double open,
        @JsonProperty("pc") double previousClose,
        @JsonProperty("t") long timestamp
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FinnhubProfile(
        @JsonProperty("name") String name,
        @JsonProperty("ticker") String ticker,
        @JsonProperty("marketCapitalization") double marketCap,
        @JsonProperty("exchange") String exchange,
        @JsonProperty("currency") String currency
    ) {}

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("demo");
    }

    public QuoteDto getQuote(String symbol) {
        if (!isConfigured()) {
            throw new IllegalStateException("Finnhub API key not configured");
        }

        try {
            // Fetch quote
            FinnhubQuote quote = restClient.get()
                .uri("https://finnhub.io/api/v1/quote?symbol={symbol}&token={token}", symbol, apiKey)
                .retrieve()
                .body(FinnhubQuote.class);

            // Fetch company profile for name/marketCap
            FinnhubProfile profile = restClient.get()
                .uri("https://finnhub.io/api/v1/stock/profile2?symbol={symbol}&token={token}", symbol, apiKey)
                .retrieve()
                .body(FinnhubProfile.class);

            String name = (profile != null && profile.name() != null) ? profile.name() : symbol;
            double marketCapM = (profile != null) ? profile.marketCap() : 0;

            return new QuoteDto(
                symbol,
                name,
                bd(quote.current()),
                bd(quote.change()),
                bd(quote.changePercent()),
                bd(quote.open()),
                bd(quote.high()),
                bd(quote.low()),
                bd(quote.previousClose()),
                null,  // volume not in Finnhub basic quote
                (long)(marketCapM * 1_000_000),
                null, null, null,
                profile != null ? profile.currency() : "USD",
                profile != null ? profile.exchange() : "US",
                quote.timestamp() > 0 ? Instant.ofEpochSecond(quote.timestamp()) : Instant.now()
            );
        } catch (Exception e) {
            log.warn("Finnhub quote failed for {}: {}", symbol, e.getMessage());
            throw e;
        }
    }

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}
