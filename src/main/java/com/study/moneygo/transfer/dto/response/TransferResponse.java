package com.study.moneygo.transfer.dto.response;

import com.study.moneygo.transaction.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class TransferResponse {

    private Long transactionId;
    private String fromAccount; // 누가 보내는지
    private String toAccount; // 어느 계좌로 보내는지
    private String toAccountOwner; // 계좌 주인이 누구인지
    private BigDecimal amount; // 송금액은 얼마인지
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private BigDecimal balanceAfter; // 거래 후 잔액

    public static TransferResponse of(Transaction transaction, String toAccountOwner, BigDecimal balanceAfter) {
        return TransferResponse.builder()
                .transactionId(transaction.getId())
                .fromAccount(transaction.getFromAccount().getAccountNumber())
                .toAccount(transaction.getToAccount().getAccountNumber())
                .toAccountOwner(toAccountOwner)
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .balanceAfter(balanceAfter)
                .build();
    }
}
