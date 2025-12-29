package com.study.moneygo.qrpayment.controller;

import com.study.moneygo.qrpayment.dto.request.QrGenerateRequest;
import com.study.moneygo.qrpayment.dto.request.QrPayRequest;
import com.study.moneygo.qrpayment.dto.response.QrGenerateResponse;
import com.study.moneygo.qrpayment.dto.response.QrPayResponse;
import com.study.moneygo.qrpayment.service.QrPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
public class QrPaymentController {

    private final QrPaymentService qrPaymentService;

    @PostMapping("/generate")
    public ResponseEntity<QrGenerateResponse> generateQrCode(@Valid @RequestBody QrGenerateRequest request) {
        QrGenerateResponse response = qrPaymentService.generateQrCode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/pay")
    public ResponseEntity<QrPayResponse> payWithQrCode(@Valid @RequestBody QrPayRequest request) {
        QrPayResponse response = qrPaymentService.payWithQrCode(request);
        return ResponseEntity.ok(response);
    }
}
