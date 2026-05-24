package com.stockiq.market.service;

import com.stockiq.market.client.AlphaVantageClient;
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
    private final AlphaVantageClient alphaVantageClient;

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
                QuoteDto quote = alphaVantageClient.getQuote(symbol);
                messagingTemplate.convertAndSend("/topic/quotes/" + symbol, quote);
            } catch (Exception e) {
                log.warn("Failed to broadcast quote for {}: {}", symbol, e.getMessage());
            }
        });
    }

    @Scheduled(fixedDelay = 5000)
    public void broadcastSubscriberQuotes() {
        subscriptions.forEach((sessionId, symbols) ->
                symbols.parallelStream().forEach(symbol -> {
                    try {
                        QuoteDto quote = alphaVantageClient.getQuote(symbol);
                        messagingTemplate.convertAndSendToUser(sessionId, "/queue/quotes", quote);
                    } catch (Exception e) {
                        log.warn("Failed to broadcast to {}: {}", sessionId, e.getMessage());
                    }
                }));
    }
}
