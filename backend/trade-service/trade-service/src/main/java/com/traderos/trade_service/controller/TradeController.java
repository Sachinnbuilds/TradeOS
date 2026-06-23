package com.traderos.trade_service.controller;

import com.traderos.trade_service.dto.TradeResponseDto;
import com.traderos.trade_service.repository.TradeRepository;
import com.traderos.trade_service.service.TradeIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeIngestionService ingestionService;
    private final TradeRepository tradeRepository;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadTrades(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a valid CSV file.");
        }

        String result = ingestionService.processCsvUpload(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analysis/{symbol}")
    public ResponseEntity<List<TradeResponseDto>> getTradeAnalysis(@PathVariable String symbol) {
        List<TradeResponseDto> trades = tradeRepository.findBySymbolOrderByTradeTimeAsc(symbol.toUpperCase())
                .stream()
                .map(t -> new TradeResponseDto(
                        t.getId(),
                        t.getSymbol(),
                        t.getTradeType(),
                        t.getQuantity(),
                        t.getRemainingQuantity(),
                        t.getPrice(),
                        t.getTradeTime(),
                        t.getRealizedPnl(),
                        t.getNetPnl(),
                        t.getBrokerage(),
                        t.getStt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(trades);
    }
}