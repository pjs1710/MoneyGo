package com.study.moneygo.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class SignupResponse {

    private Long userId;
    private String email;
    private String name;
    private String accountNumber;
    private BigDecimal balance;
    private String message;

    public static SignupResponse of(
            Long userId, String email, String namme, String accountNumber, BigDecimal balance
            ) {
        return SignupResponse.builder()
                .userId(userId)
                .email(email)
                .name(namme)
                .accountNumber(accountNumber)
                .balance(balance)
                .message("회원가입이 완료되었습니다.")
                .build();
    }
}
