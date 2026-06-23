package com.traderos.trade_service.domain.event;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TradeEvent(
        UUID eventId,
        String eventType,
        UUID tradeId,
        String symbol,
        String instrumentType,
        String tradeType,
        Integer quantity,
        BigDecimal price,
        LocalDateTime tradeTime,
        BigDecimal realizedPnl,
        LocalDateTime occurredAt
) {}