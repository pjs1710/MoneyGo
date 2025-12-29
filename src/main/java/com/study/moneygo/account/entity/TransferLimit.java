package com.study.moneygo.account.entity;

import com.study.moneygo.util.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transfer_limits")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferLimit extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "daily_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit = new BigDecimal("3000000.00"); // 일일 한도는 300만원으로 고정

    @Column(name = "per_transaction_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal perTransactionLimit = new BigDecimal("1000000.00"); // 1회 한도 100만원

    @Column(name = "today_used", nullable = false, precision = 15, scale = 2)
    private BigDecimal todayUsed = BigDecimal.ZERO;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate = LocalDate.now();

    /**
     * =====================================
     * 비즈니스 메서드
     * =====================================
     */

    public void resetIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastResetDate)) {
            this.todayUsed = BigDecimal.ZERO;
            this.lastResetDate = today;
        }
    }

    public boolean canTransfer(BigDecimal amount) {
        resetIfNewDay();

        // 1회 한도 체크 (100만원)
        if (amount.compareTo(perTransactionLimit) > 0) {
            return false;
        }

        // 일일 한도 체크 (300만원)
        BigDecimal afterTransfer = todayUsed.add(amount);
        return afterTransfer.compareTo(dailyLimit) <= 0;
    }

    public void addUsage(BigDecimal amount) {
        resetIfNewDay();
        this.todayUsed = this.todayUsed.add(amount);
    }

    public BigDecimal getRemainingDailyLimit() {
        resetIfNewDay();
        return dailyLimit.subtract(todayUsed);
    }
}
