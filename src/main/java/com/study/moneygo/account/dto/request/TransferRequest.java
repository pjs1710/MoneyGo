package com.study.moneygo.account.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "받는 계좌번호는 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}$", message = "계좌번호 형식이 올바르지 않습니다. (예: 1001-1234-5678)")
    private String toAccountNumber;

    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야합니다.")
    @DecimalMax(value = "1000000.00", message = "1회 최대 송금액은 100만원입니다.")
    private BigDecimal amount;

    @Size(max = 200, message = "메모는 200자 이내로 입력해주세요.")
    private String description;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password; // 간편 비밀번호 OR 거래 비밀번호
}
