package com.study.moneygo.scheduled.transfer.dto.response;

import com.study.moneygo.scheduled.transfer.entity.ScheduledTransfer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ScheduledTransferResponse {

    private Long scheduleId;
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
    private LocalDateTime scheduledAt;
    private String status;
    private Long executedTransactionId;
    private LocalDateTime executionAttemptedAt;
    private String failureReason;
    private LocalDateTime createdAt;

    /*
    Response.of 메서드
     */
    public static ScheduledTransferResponse of(ScheduledTransfer schedule) {
        return ScheduledTransferResponse.builder()
                .scheduleId(schedule.getId())
                .toAccountNumber(schedule.getToAccountNumber())
                .amount(schedule.getAmount())
                .description(schedule.getDescription())
                .scheduledAt(schedule.getScheduledAt())
                .status(schedule.getStatus().name())
                .executedTransactionId(
                        schedule.getExecutedTransaction() != null
                                ? schedule.getExecutedTransaction().getId()
                                : null
                )
                .executionAttemptedAt(schedule.getExecutionAttemptedAt())
                .failureReason(schedule.getFailureReason())
                .createdAt(schedule.getCreatedAt())
                .build();
    }
}
