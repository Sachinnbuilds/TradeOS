package com.traderos.trade_service.service;

import com.traderos.trade_service.domain.entity.Trade;
import com.traderos.trade_service.domain.event.TradeEvent;
import com.traderos.trade_service.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PnlComputationService {

    private final TradeRepository tradeRepository;

    @Transactional
    public void calculateRealizedPnl(TradeEvent event) {
        // We only calculate realized P&L when an asset is SOLD
        if (!"SELL".equalsIgnoreCase(event.tradeType())) {
            return;
        }

        Trade sellTrade = tradeRepository.findById(event.tradeId())
                .orElseThrow(() -> new RuntimeException("Trade not found for ID: " + event.tradeId()));

        List<Trade> openBuys = tradeRepository
                .findBySymbolAndTradeTypeAndRemainingQuantityGreaterThanOrderByTradeTimeAsc(
                        event.symbol(), "BUY", 0);

        int quantityToSell = sellTrade.getQuantity();
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;

        for (Trade buyTrade : openBuys) {
            if (quantityToSell <= 0) break;

            int matchedQuantity = Math.min(quantityToSell, buyTrade.getRemainingQuantity());

            // P&L = (Sell Price - Buy Price) * matched quantity
            BigDecimal priceDifference = sellTrade.getPrice().subtract(buyTrade.getPrice());
            BigDecimal lotPnl = priceDifference.multiply(BigDecimal.valueOf(matchedQuantity));

            totalRealizedPnl = totalRealizedPnl.add(lotPnl);

            // Deduct matched quantities
            buyTrade.setRemainingQuantity(buyTrade.getRemainingQuantity() - matchedQuantity);
            quantityToSell -= matchedQuantity;

            tradeRepository.save(buyTrade);
        }

        // Apply charges (Brokerage + STT mockup based on PRD F2.3)
        BigDecimal brokerage = calculateBrokerage(sellTrade);
        BigDecimal stt = calculateStt(sellTrade);
        BigDecimal netPnl = totalRealizedPnl.subtract(brokerage).subtract(stt);

        sellTrade.setRealizedPnl(totalRealizedPnl);
        sellTrade.setBrokerage(brokerage);
        sellTrade.setStt(stt);
        sellTrade.setNetPnl(netPnl);
        sellTrade.setRemainingQuantity(quantityToSell); // Should be 0 if fully matched

        tradeRepository.save(sellTrade);
        log.info("Calculated P&L for Trade {}: Realized={}, Net={}", sellTrade.getId(), totalRealizedPnl, netPnl);
    }

    private BigDecimal calculateBrokerage(Trade trade) {
        // Standard Zerodha flat ₹20 or 0.03% whichever is lower per order (simplified)
        BigDecimal percentageBased = trade.getPrice()
                .multiply(BigDecimal.valueOf(trade.getQuantity()))
                .multiply(BigDecimal.valueOf(0.0003));
        return percentageBased.min(BigDecimal.valueOf(20.00)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateStt(Trade trade) {
        // Simplified STT mockup (0.0125% for futures/options)
        return trade.getPrice()
                .multiply(BigDecimal.valueOf(trade.getQuantity()))
                .multiply(BigDecimal.valueOf(0.000125))
                .setScale(2, RoundingMode.HALF_UP);
    }
}