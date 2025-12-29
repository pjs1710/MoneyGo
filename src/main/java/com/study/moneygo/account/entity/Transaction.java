package com.study.moneygo.account.entity;

import com.study.moneygo.util.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount; // null이면 충전

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount; // null이면 인출

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 200)
    private String description;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey; // 중복 요청 방지

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * =====================================
     * 비즈니스 메서드
     * =====================================
     */

    public void complete() {
        this.status = TransactionStatus.COMPLETED;
    }

    public void fail(String errorMessage) {
        this.status = TransactionStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return this.status == TransactionStatus.COMPLETED;
    }

    public enum TransactionType {
        TRANSFER,      // 송금
        DEPOSIT,       // 입금 (충전)
        WITHDRAW,    // 출금
        QR_PAYMENT     // QR 결제
    }

    public enum TransactionStatus {
        PENDING,       // 처리 중
        COMPLETED,     // 완료
        FAILED,        // 실패
        CANCELLED      // 취소
    }
}
