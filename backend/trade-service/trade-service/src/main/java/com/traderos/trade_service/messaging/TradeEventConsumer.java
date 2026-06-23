package com.traderos.trade_service.messaging;

import com.traderos.trade_service.domain.event.TradeEvent;
import com.traderos.trade_service.service.PnlComputationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeEventConsumer {

    private final PnlComputationService pnlComputationService;

    @KafkaListener(topics = "trade.ingested", groupId = "trade-service-pnl-group")
    public void consumeTradeEvent(TradeEvent event) {
        log.debug("Consumed TradeEvent for P&L computation: {}", event.tradeId());
        try {
            pnlComputationService.calculateRealizedPnl(event);
        } catch (Exception e) {
            // In a production system, we would route this to a Dead Letter Queue (DLQ)
            log.error("Failed to compute P&L for trade: {}", event.tradeId(), e);
        }
    }
}