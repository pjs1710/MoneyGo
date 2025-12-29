package com.study.moneygo.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class AdminWithdrawResponse {

    private String accountNumber;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal amount;
    private String message;

    public static AdminWithdrawResponse of(
            String accountNumber, BigDecimal balanceBefore, BigDecimal balanceAfter, BigDecimal amount) {
        return AdminWithdrawResponse.builder()
                .accountNumber(accountNumber)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .amount(amount)
                .message("인출이 완료되었습니다.")
                .build();
    }
}
