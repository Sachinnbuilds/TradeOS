package com.traderos.trade_service.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trade_hash", unique = true, nullable = false, length = 64)
    private String tradeHash; // SHA-256 for deduplication

    @Column(nullable = false, length = 50)
    private String symbol;

    @Column(name = "instrument_type", nullable = false, length = 20)
    private String instrumentType; // EQ, FUT, CE, PE

    @Column(name = "trade_type", nullable = false, length = 10)
    private String tradeType; // BUY, SELL

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal price;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "trade_time", nullable = false)
    private LocalDateTime tradeTime;

    @Column(nullable = false, length = 10)
    private String exchange;

    @Column(name = "strategy_tag", length = 100)
    private String strategyTag;

    @Column(name = "emotion_tag", length = 50)
    private String emotionTag;

    @Column(name = "mistake_type", length = 100)
    private String mistakeType;

    @Column(name = "realized_pnl", precision = 12, scale = 2)
    private BigDecimal realizedPnl;

    @Column(precision = 10, scale = 4)
    private BigDecimal brokerage;

    @Column(precision = 10, scale = 4)
    private BigDecimal stt;

    @Column(name = "net_pnl", precision = 12, scale = 2)
    private BigDecimal netPnl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}