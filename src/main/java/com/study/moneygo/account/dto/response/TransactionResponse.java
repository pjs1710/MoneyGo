package com.study.moneygo.account.dto.response;

import com.study.moneygo.account.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long transactionId;
    private String type;
    private BigDecimal amount;
    private String fromAccount;
    private String toAccount;
    private String counterpartyName; // 상대방 이름
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public static TransactionResponse of(Transaction transaction, Long myAccountId, String counterpartyName) {
        String fromAccount = transaction.getFromAccount() != null ?
                transaction.getFromAccount().getAccountNumber() : null;
        String toAccount = transaction.getToAccount() != null ?
                transaction.getToAccount().getAccountNumber() : null;

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .counterpartyName(counterpartyName)
                .description(transaction.getDescription())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
