package com.study.moneygo.deposit.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfDepositRequest {

    @NotNull(message = "입금 금액은 필수입니다.")
    @DecimalMin(value = "1000", message = "최소 입금 금액은 1,000원 입니다.")
    private BigDecimal amount;

    @NotNull(message = "간편 비밀번호는 필수입니다.")
    @Size(min = 6, max = 6)
    @Pattern(regexp = "^\\d{6}$", message = "간편 비밀번호는 6자리 숫자여야 합니다.")
    private String simplePassword;

    private String description; // 입금 메모는 선택
}
