package com.traderos.trade_service.service;

import com.traderos.trade_service.domain.entity.Trade;
import com.traderos.trade_service.domain.event.TradeEvent;
import com.traderos.trade_service.messaging.TradeEventPublisher;
import com.traderos.trade_service.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeIngestionService {

    private final TradeRepository tradeRepository;
    private final TradeEventPublisher eventPublisher;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Transactional
    public String processCsvUpload(MultipartFile file) {
        int successCount = 0;
        int duplicateCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // Skip the header row
                    continue;
                }

                // Generates a unique hash for the raw row to enforce idempotency
                String tradeHash = generateSha256(line);

                if (tradeRepository.existsByTradeHash(tradeHash)) {
                    duplicateCount++;
                    continue; // Skip duplicate trades
                }

                Trade trade = parseCsvLine(line, tradeHash);
                trade = tradeRepository.save(trade);

                publishEvent(trade);
                successCount++;
            }
        } catch (Exception e) {
            log.error("Failed to process CSV file", e);
            throw new RuntimeException("CSV Processing failed: " + e.getMessage());
        }

        return String.format("Upload complete. Ingested: %d, Duplicates ignored: %d", successCount, duplicateCount);
    }

    private Trade parseCsvLine(String line, String hash) {
        // Expected CSV format: symbol, instrumentType, tradeType, quantity, price, tradeDate, tradeTime, exchange
        String[] columns = line.split(",");

        // Parse quantity once so we can use it for both total and remaining
        int parsedQuantity = Integer.parseInt(columns[3].trim());

        return Trade.builder()
                .tradeHash(hash)
                .symbol(columns[0].trim())
                .instrumentType(columns[1].trim())
                .tradeType(columns[2].trim())
                .quantity(parsedQuantity)
                .remainingQuantity(parsedQuantity) // Initializes the open lot size for FIFO matching
                .price(new BigDecimal(columns[4].trim()))
                .tradeDate(LocalDate.parse(columns[5].trim(), DATE_FORMATTER))
                .tradeTime(LocalDateTime.parse(columns[6].trim(), TIME_FORMATTER))
                .exchange(columns[7].trim())
                .build();
    }

    private void publishEvent(Trade trade) {
        TradeEvent event = new TradeEvent(
                java.util.UUID.randomUUID(),
                "TRADE_INGESTED",
                trade.getId(),
                trade.getSymbol(),
                trade.getInstrumentType(),
                trade.getTradeType(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getTradeTime(),
                null, // Realized PNL is calculated later
                LocalDateTime.now()
        );
        eventPublisher.publish(event);
    }

    private String generateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing trade row", e);
        }
    }
}