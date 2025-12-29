package com.study.moneygo.account.dto.response;

import com.study.moneygo.account.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class AccountResponse {

    private Long accountId;
    private String accountNumber;
    private BigDecimal balance;
    private String status;
    private LocalDateTime createdAt;

    public static AccountResponse of(Account account) {
        return AccountResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
