package com.study.moneygo.notification.entity;

import com.study.moneygo.user.entity.User;
import com.study.moneygo.util.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "related_transaction_id")
    private Long relatedTransactionId;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public enum NotificationType {
        TRANSFER_RECEIVED,              // 송금 받음
        TRANSFER_SENT,                  // 송금 완료
        SCHEDULED_TRANSFER_EXECUTED,    // 예약 송금 실행
        SCHEDULED_TRANSFER_FAILED,      // 예약 송금 실패
        QR_PAYMENT_RECEIVED,            // QR 결제 받음
        QR_PAYMENT_SENT,                // QR 결제 완료
        ACCOUNT_LOCKED,                 // 계정 잠김
        LARGE_AMOUNT_ALERT              // 고액 거래 알림
    }

    /** =====================================
     *              비즈니스 메서드
     *  ===================================== */

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
