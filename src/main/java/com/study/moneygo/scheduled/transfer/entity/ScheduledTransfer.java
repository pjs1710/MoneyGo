package com.study.moneygo.scheduled.transfer.entity;

import com.study.moneygo.account.entity.Account;
import com.study.moneygo.transaction.entity.Transaction;
import com.study.moneygo.util.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_transfers")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledTransfer extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @Column(name = "to_account_number", nullable = false, length = 20)
    private String toAccountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 200)
    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleStatus status = ScheduleStatus.PENDING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_transaction_id")
    private Transaction executedTransaction;

    @Column(name = "execution_attempted_at")
    private LocalDateTime executionAttemptedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public enum ScheduleStatus {
        PENDING,    // 대기 중
        EXECUTED,   // 실행 완료
        FAILED,     // 실행 실패
        CANCELLED   // 취소됨
    }

    /** =====================================
     *              비즈니스 메서드
     *  ===================================== */

    public void execute(Transaction transaction) {
        this.status = ScheduleStatus.EXECUTED;
        this.executedTransaction = transaction;
        this.executionAttemptedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status = ScheduleStatus.FAILED;
        this.failureReason = reason;
        this.executionAttemptedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == ScheduleStatus.PENDING) {
            this.status = ScheduleStatus.CANCELLED;
        } else {
            throw new IllegalStateException("대기 중인 예약만 취소할 수 있습니다.");
        }
    }

    public boolean isPending() {
        return this.status == ScheduleStatus.PENDING;
    }

    public boolean isReadyToExecute() {
        return isPending() && LocalDateTime.now().isAfter(scheduledAt);
    }
}
