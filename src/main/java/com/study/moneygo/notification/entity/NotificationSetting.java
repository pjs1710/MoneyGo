package com.study.moneygo.notification.entity;

import com.study.moneygo.user.entity.User;
import com.study.moneygo.util.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 이메일 알림 설정
    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "transfer_received_email", nullable = false)
    private boolean transferReceivedEmail = true;

    @Column(name = "transfer_sent_email", nullable = false)
    private boolean transferSentEmail = false;

    @Column(name = "scheduled_transfer_email", nullable = false)
    private boolean scheduledTransferEmail = true;

    @Column(name = "qr_payment_email", nullable = false)
    private boolean qrPaymentEmail = true;

    // 고액 거래 알림
    @Column(name = "large_amount_alert_enabled", nullable = false)
    private boolean largeAmountAlertEnabled = true;

    @Column(name = "large_amount_threshold", precision = 15, scale = 2)
    private BigDecimal largeAmountThreshold = new BigDecimal("500000");  // 기본 50만원

    /** =====================================
     *              비즈니스 메서드
     *  ===================================== */

    public void updateEmailEnabled(boolean enabled) {
        this.emailEnabled = enabled;
    }

    public void updateTransferReceivedEmail(boolean enabled) {
        this.transferReceivedEmail = enabled;
    }

    public void updateTransferSentEmail(boolean enabled) {
        this.transferSentEmail = enabled;
    }

    public void updateScheduledTransferEmail(boolean enabled) {
        this.scheduledTransferEmail = enabled;
    }

    public void updateQrPaymentEmail(boolean enabled) {
        this.qrPaymentEmail = enabled;
    }

    public void updateLargeAmountAlert(boolean enabled, BigDecimal threshold) {
        this.largeAmountAlertEnabled = enabled;
        if (threshold != null) {
            this.largeAmountThreshold = threshold;
        }
    }

    public boolean shouldNotify(Notification.NotificationType type) {
        if (!emailEnabled) {
            return false;
        }

        return switch (type) {
            case TRANSFER_RECEIVED -> transferReceivedEmail;
            case TRANSFER_SENT -> transferSentEmail;
            case SCHEDULED_TRANSFER_EXECUTED, SCHEDULED_TRANSFER_FAILED -> scheduledTransferEmail;
            case QR_PAYMENT_RECEIVED, QR_PAYMENT_SENT -> qrPaymentEmail;
            case LARGE_AMOUNT_ALERT -> largeAmountAlertEnabled;
            default -> false;
        };
    }

    public boolean isLargeAmount(BigDecimal amount) {
        return largeAmountAlertEnabled &&
                amount.compareTo(largeAmountThreshold) >= 0;
    }
}
