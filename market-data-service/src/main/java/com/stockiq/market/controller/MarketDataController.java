package com.stockiq.market.controller;

import com.stockiq.market.client.MarketDataClient;
import com.stockiq.market.dto.QuoteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataClient marketDataClient;

    @GetMapping("/quote/{symbol}")
    public ResponseEntity<QuoteDto> getQuote(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataClient.getQuote(symbol.toUpperCase()));
    }

    @GetMapping("/quotes")
    public ResponseEntity<List<QuoteDto>> getBatchQuotes(@RequestParam List<String> symbols) {
        List<QuoteDto> quotes = symbols.parallelStream()
                .map(s -> marketDataClient.getQuote(s.toUpperCase()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(quotes);
    }

    @GetMapping("/mag7")
    public ResponseEntity<List<QuoteDto>> getMag7() {
        return getBatchQuotes(List.of("AAPL", "MSFT", "NVDA", "GOOGL", "AMZN", "META", "TSLA"));
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<QuoteDto>> getWatchlist(@RequestParam List<String> symbols) {
        return getBatchQuotes(symbols);
    }

    @GetMapping("/crypto")
    public ResponseEntity<List<QuoteDto>> getCrypto() {
        return getBatchQuotes(List.of("BTC", "ETH", "BNB", "SOL", "XRP"));
    }
}
