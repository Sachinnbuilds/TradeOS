package com.traderos.trade_service.messaging;

import com.traderos.trade_service.domain.event.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeEventPublisher {

    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
    private static final String TOPIC = "trade.ingested";

    public void publish(TradeEvent event) {
        // Using the tradeId as the routing key ensures events for the same trade stay in order
        kafkaTemplate.send(TOPIC, event.tradeId().toString(), event);
        log.debug("Published TradeEvent for Trade ID: {}", event.tradeId());
    }
}