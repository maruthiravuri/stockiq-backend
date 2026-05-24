package com.stockiq.market.websocket;

import com.stockiq.market.service.QuoteBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class QuoteWebSocketController {

    private final QuoteBroadcastService broadcastService;

    @MessageMapping("/subscribe")
    public void subscribe(@Header("simpSessionId") String sessionId,
                          @Payload List<String> symbols) {
        log.info("WS session {} subscribing to symbols: {}", sessionId, symbols);
        broadcastService.subscribe(sessionId, symbols);
    }

    @MessageMapping("/unsubscribe")
    public void unsubscribe(@Header("simpSessionId") String sessionId) {
        log.info("WS session {} unsubscribing", sessionId);
        broadcastService.unsubscribe(sessionId);
    }
}
