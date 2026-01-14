package com.study.moneygo.account.controller;


import com.study.moneygo.account.dto.response.TransactionResponse;
import com.study.moneygo.account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /*
    거래 내역 불러오기
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam(required = false) String type, // ALL, SENT, RECEIVED
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TransactionResponse> responses = transactionService.getTransactions(type, startDate, endDate, pageable);
        return ResponseEntity.ok(responses);
    }

    /*
    거래 내역 상세 조회
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionDetail(@PathVariable Long transactionId) {
        TransactionResponse response = transactionService.getTransactionDetail(transactionId);
        return ResponseEntity.ok(response);
    }

    /*
    거래 내역 필터링 조회
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<TransactionResponse>> getFilteredTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("거래 내역 필터링 조회: startDate={}, endDate={}, type={}", startDate, endDate, type);
        Page<TransactionResponse> transactions = transactionService.getFilteredTransactions(
                startDate, endDate, type, pageable);
        return ResponseEntity.ok(transactions);
    }
}
