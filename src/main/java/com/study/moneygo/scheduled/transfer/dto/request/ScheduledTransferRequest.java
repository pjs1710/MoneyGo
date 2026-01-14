package com.study.moneygo.scheduled.transfer.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTransferRequest {

    @NotBlank(message = "받는 계좌번호는 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}$", message = "올바른 계좌번호 형식이 아닙니다.")
    private String toAccountNumber;

    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다.")
    @DecimalMax(value = "1000000.00", message = "송금 예약 최대 금액은 100만원입니다.")
    private BigDecimal amount;

    @Size(max = 200, message = "설명은 200자 이내로 입력해주세요.")
    private String description;

    @NotNull(message = "예약 시간은 필수입니다.")
    @Future(message = "예약 시간은 현재 시간 이후여야 합니다.")
    private LocalDateTime scheduledAt;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "간편 비밀번호는 6자리 숫자여야 합니다.")
    private String simplePassword;
}
