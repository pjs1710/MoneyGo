package com.study.moneygo.transaction.controller;


import com.study.moneygo.transaction.dto.request.ReceiptEmailRequest;
import com.study.moneygo.transaction.dto.response.TransactionResponse;
import com.study.moneygo.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    /*
    거래 영수증 PDF 다운로드
     */
    @GetMapping("/{transactionId}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long transactionId) {
        log.info("거래 영수증 다운로드 요청: transactionId={}", transactionId);
        byte[] pdf = transactionService.generateReceipt(transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "receipt_" + transactionId + ".pdf");

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /*
    거래 영수증 이메일 발송
     */
    @PostMapping("/{transactionId}/receipt/email")
    public ResponseEntity<Void> sendReceiptEmail(
            @PathVariable Long transactionId,
            @RequestBody ReceiptEmailRequest request
    ) {
        log.info("거래 영수증 이메일 발송 요청: transactionId={}, email={}", transactionId, request.getEmail());

        transactionService.sendReceiptEmail(transactionId, request.getEmail());
        return ResponseEntity.ok().build();
    }

    /*
    거래 내역서 PDF 다운로드 (월별/연도별)
     */
    @GetMapping("/statement")
    public ResponseEntity<byte[]> downloadStatement(
            @RequestParam Integer year,
            @RequestParam(required = false) Integer month
    ) {
        log.info("거래 내역서 다운로드 요청: year={}, month={}", year, month);
        byte[] pdf = transactionService.generateStatement(year, month);

        String filename = month != null
                ? String.format("statement_%d_%02d.pdf", year, month)
                : String.format("statement_%d.pdf", year);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
