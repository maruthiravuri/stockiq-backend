package com.stockiq.market.service;

import com.stockiq.market.client.MarketDataClient;
import com.stockiq.market.dto.QuoteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MarketDataClient marketDataClient;

    private final Map<String, List<String>> subscriptions = new ConcurrentHashMap<>();

    private static final List<String> DEFAULT_SYMBOLS =
            List.of("AAPL", "MSFT", "NVDA", "GOOGL", "AMZN", "META", "TSLA",
                    "VOO", "GLD", "SCHD", "XLE", "QQQM");

    public void subscribe(String sessionId, List<String> symbols) {
        subscriptions.put(sessionId, new CopyOnWriteArrayList<>(symbols));
        log.debug("Session {} subscribed to: {}", sessionId, symbols);
    }

    public void unsubscribe(String sessionId) {
        subscriptions.remove(sessionId);
    }

    @Scheduled(fixedDelay = 5000)
    public void broadcastDefaultQuotes() {
        DEFAULT_SYMBOLS.parallelStream().forEach(symbol -> {
            try {
                QuoteDto quote = marketDataClient.getQuote(symbol);
                messagingTemplate.convertAndSend("/topic/quotes/" + symbol, quote);
            } catch (Exception e) {
                // Normal when no WebSocket clients are connected - use debug level
                log.debug("Broadcast skipped for {} (no subscribers): {}", symbol, e.getMessage());
            }
        });
    }

    @Scheduled(fixedDelay = 5000)
    public void broadcastSubscriberQuotes() {
        if (subscriptions.isEmpty()) return; // skip entirely if no subscribers
        subscriptions.forEach((sessionId, symbols) ->
                symbols.parallelStream().forEach(symbol -> {
                    try {
                        QuoteDto quote = marketDataClient.getQuote(symbol);
                        messagingTemplate.convertAndSendToUser(sessionId, "/queue/quotes", quote);
                    } catch (Exception e) {
                        log.debug("Broadcast to {} skipped: {}", sessionId, e.getMessage());
                    }
                }));
    }
}
