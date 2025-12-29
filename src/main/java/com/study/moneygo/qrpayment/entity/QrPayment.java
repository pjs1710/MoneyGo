package com.study.moneygo.qrpayment.entity;

import com.study.moneygo.account.entity.Account;
import com.study.moneygo.account.entity.Transaction;
import com.study.moneygo.util.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_payments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_account_id", nullable = false)
    private Account sellerAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "qr_code", nullable = false, unique = true, length = 100)
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QrPaymentStatus status = QrPaymentStatus.PENDING;

    @Column(length = 200)
    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** =====================================
     *              비즈니스 메서드
     *  ===================================== */

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isPending() {
        return this.status == QrPaymentStatus.PENDING;
    }

    public void complete(Transaction transaction) {
        this.status = QrPaymentStatus.COMPLETED;
        this.transaction = transaction;
    }

    public void expire() {
        this.status = QrPaymentStatus.EXPIRED;
    }

    public void cancel() {
        this.status = QrPaymentStatus.CANCELLED;
    }

    public enum QrPaymentStatus {
        PENDING,    // 대기중
        COMPLETED,  // 완료
        EXPIRED,    // 만료
        CANCELLED   // 취소
    }
}
