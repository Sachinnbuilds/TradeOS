package com.traderos.trade_service.repository;
import com.traderos.trade_service.domain.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {

    // Checks if a trade has already been ingested to prevent duplicates
    boolean existsByTradeHash(String tradeHash);
    // Fetches available lots for FIFO matching
    List<Trade> findBySymbolAndTradeTypeAndRemainingQuantityGreaterThanOrderByTradeTimeAsc(
            String symbol, String tradeType, Integer remainingQuantity);

    List<Trade> findBySymbolOrderByTradeTimeAsc(String symbol);
}