package com.study.moneygo.qrpayment.dto.response;

import com.study.moneygo.transaction.entity.Transaction;
import com.study.moneygo.qrpayment.entity.QrPayment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class QrPayResponse {

    private Long transactionId;
    private Long qrPaymentId;
    private BigDecimal amount;
    private String description;
    private String sellerAccount;
    private String sellerName;
    private String status;
    private LocalDateTime createdAt;
    private BigDecimal balanceAfter;

    public static QrPayResponse of(QrPayment qrPayment, Transaction transaction,
                                   String sellerName, BigDecimal balanceAfter) {
        return QrPayResponse.builder()
                .transactionId(transaction.getId())
                .qrPaymentId(qrPayment.getId())
                .amount(qrPayment.getAmount())
                .description(qrPayment.getDescription())
                .sellerAccount(qrPayment.getSellerAccount().getAccountNumber())
                .sellerName(sellerName)
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .balanceAfter(balanceAfter)
                .build();
    }
}
