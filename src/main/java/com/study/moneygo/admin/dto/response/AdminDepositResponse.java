package com.study.moneygo.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class AdminDepositResponse {

    private String accountNumber;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal amount;
    private String message;

    public static AdminDepositResponse of(String accountNumber, BigDecimal balanceBefore, BigDecimal balanceAfter, BigDecimal amount
    ) {
        return AdminDepositResponse.builder()
                .accountNumber(accountNumber)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .amount(amount)
                .message("충전이 완료되었습니다.")
                .build();
    }
}
