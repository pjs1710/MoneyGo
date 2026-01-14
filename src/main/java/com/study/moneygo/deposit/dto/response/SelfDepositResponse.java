package com.study.moneygo.deposit.dto.response;

import com.study.moneygo.transaction.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfDepositResponse {

    private Long transactionId;
    private String accountNumber;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime depositAt;

    public static SelfDepositResponse of(Transaction transaction, BigDecimal balanceAfter) {
        return SelfDepositResponse.builder()
                .transactionId(transaction.getId())
                .accountNumber(transaction.getToAccount().getAccountNumber())
                .amount(transaction.getAmount())
                .balanceAfter(balanceAfter)
                .description(transaction.getDescription())
                .depositAt(transaction.getCreatedAt())
                .build();
    }
}
