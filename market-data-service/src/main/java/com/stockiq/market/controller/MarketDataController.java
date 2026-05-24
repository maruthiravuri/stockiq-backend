package com.stockiq.market.controller;

import com.stockiq.market.client.MarketDataClient;
import com.stockiq.market.dto.QuoteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataClient marketDataClient;

    private static final List<String> MAG7 =
        List.of("AAPL", "MSFT", "NVDA", "GOOGL", "AMZN", "META", "TSLA");

    private static final List<String> CRYPTO =
        List.of("BTCUSD", "ETHUSD", "SOLUSD", "XRPUSD", "ADAUSD");

    @GetMapping("/quote/{symbol}")
    public ResponseEntity<QuoteDto> getQuote(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataClient.getQuote(symbol.toUpperCase()));
    }

    @GetMapping("/quotes")
    public ResponseEntity<List<QuoteDto>> getBatchQuotes(@RequestParam List<String> symbols) {
        // Use batch endpoint — single API call to Alpaca for all symbols
        return ResponseEntity.ok(marketDataClient.getBatchQuotes(
            symbols.stream().map(String::toUpperCase).toList()
        ));
    }

    @GetMapping("/mag7")
    public ResponseEntity<List<QuoteDto>> getMag7() {
        return ResponseEntity.ok(marketDataClient.getBatchQuotes(MAG7));
    }

    @GetMapping("/crypto")
    public ResponseEntity<List<QuoteDto>> getCrypto() {
        return ResponseEntity.ok(marketDataClient.getBatchQuotes(CRYPTO));
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<QuoteDto>> getWatchlist(@RequestParam List<String> symbols) {
        return ResponseEntity.ok(marketDataClient.getBatchQuotes(
            symbols.stream().map(String::toUpperCase).toList()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
