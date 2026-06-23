package com.traderos.trade_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TradeResponseDto(
        UUID id,
        String symbol,
        String tradeType,
        Integer quantity,
        Integer remainingQuantity,
        BigDecimal price,
        LocalDateTime tradeTime,
        BigDecimal realizedPnl,
        BigDecimal netPnl,
        BigDecimal brokerage,
        BigDecimal stt
) {}