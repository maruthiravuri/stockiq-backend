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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlpacaClient {

    private final RestClient restClient;

    @Value("${market.alpaca.api-key:#{null}}")
    private String apiKey;

    @Value("${market.alpaca.api-secret:#{null}}")
    private String apiSecret;

    private static final String BASE_URL = "https://data.alpaca.markets/v2";

    // ── Response types ──────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AlpacaBar(
        @JsonProperty("t") String timestamp,
        @JsonProperty("o") double open,
        @JsonProperty("h") double high,
        @JsonProperty("l") double low,
        @JsonProperty("c") double close,
        @JsonProperty("v") long volume,
        @JsonProperty("vw") double vwap
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AlpacaSnapshot(
        @JsonProperty("latestTrade") AlpacaTrade latestTrade,
        @JsonProperty("latestQuote") AlpacaQuote latestQuote,
        @JsonProperty("minuteBar") AlpacaBar minuteBar,
        @JsonProperty("dailyBar") AlpacaBar dailyBar,
        @JsonProperty("prevDailyBar") AlpacaBar prevDailyBar
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AlpacaTrade(
        @JsonProperty("p") double price,
        @JsonProperty("s") long size,
        @JsonProperty("t") String timestamp
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AlpacaQuote(
        @JsonProperty("ap") double askPrice,
        @JsonProperty("bp") double bidPrice,
        @JsonProperty("as") int askSize,
        @JsonProperty("bs") int bidSize
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SnapshotsResponse(
        @JsonProperty("snapshots") Map<String, AlpacaSnapshot> snapshots
    ) {}

    // ── Public methods ──────────────────────────────────────────────────────

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
            && apiSecret != null && !apiSecret.isBlank();
    }

    /**
     * Get a single real-time quote via the snapshot endpoint.
     * Uses the latest trade price as the current price.
     */
    public QuoteDto getQuote(String symbol) {
        AlpacaSnapshot snap = fetchSnapshot(symbol);
        return toQuoteDto(symbol, snap);
    }

    /**
     * Batch fetch up to 100 symbols in one API call — very efficient.
     */
    public List<QuoteDto> getBatchQuotes(List<String> symbols) {
        String joined = String.join(",", symbols);

        @SuppressWarnings("unchecked")
        Map<String, AlpacaSnapshot> snapshots = restClient.get()
            .uri(BASE_URL + "/stocks/snapshots?symbols={symbols}&feed=iex", joined)
            .header("APCA-API-KEY-ID", apiKey)
            .header("APCA-API-SECRET-KEY", apiSecret)
            .retrieve()
            .body(Map.class);

        if (snapshots == null) return List.of();

        return symbols.stream()
            .filter(snapshots::containsKey)
            .map(sym -> toQuoteDto(sym, parseSnapshot(snapshots.get(sym))))
            .collect(Collectors.toList());
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private AlpacaSnapshot fetchSnapshot(String symbol) {
        return restClient.get()
            .uri(BASE_URL + "/stocks/{symbol}/snapshot?feed=iex", symbol)
            .header("APCA-API-KEY-ID", apiKey)
            .header("APCA-API-SECRET-KEY", apiSecret)
            .retrieve()
            .body(AlpacaSnapshot.class);
    }

    @SuppressWarnings("unchecked")
    private AlpacaSnapshot parseSnapshot(Object raw) {
        // When deserializing from Map<String, Object>, convert manually
        if (raw instanceof AlpacaSnapshot s) return s;
        return null;
    }

    private QuoteDto toQuoteDto(String symbol, AlpacaSnapshot snap) {
        if (snap == null) throw new RuntimeException("No snapshot for " + symbol);

        double price = snap.latestTrade() != null ? snap.latestTrade().price()
                     : snap.minuteBar() != null   ? snap.minuteBar().close()
                     : 0;

        double prevClose = snap.prevDailyBar() != null ? snap.prevDailyBar().close() : price;
        double open      = snap.dailyBar()     != null ? snap.dailyBar().open()      : price;
        double high      = snap.dailyBar()     != null ? snap.dailyBar().high()      : price;
        double low       = snap.dailyBar()     != null ? snap.dailyBar().low()       : price;
        long   volume    = snap.dailyBar()     != null ? snap.dailyBar().volume()    : 0;

        double change    = price - prevClose;
        double changePct = prevClose > 0 ? (change / prevClose) * 100 : 0;

        Instant ts = snap.latestTrade() != null && snap.latestTrade().timestamp() != null
            ? Instant.parse(snap.latestTrade().timestamp())
            : Instant.now();

        return new QuoteDto(
            symbol, symbol,           // name resolved separately if needed
            bd(price),
            bd(change),
            bd(changePct),
            bd(open), bd(high), bd(low), bd(prevClose),
            volume,
            null, null, null, null,
            "USD", "US",
            ts
        );
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }
}
