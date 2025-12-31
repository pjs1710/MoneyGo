package com.study.moneygo.notification.service;

import com.study.moneygo.account.entity.Transaction;
import com.study.moneygo.notification.dto.response.NotificationResponse;
import com.study.moneygo.notification.entity.Notification;
import com.study.moneygo.notification.entity.NotificationSetting;
import com.study.moneygo.notification.repository.NotificationRepository;
import com.study.moneygo.notification.repository.NotificationSettingRepository;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public void createTransferNotification(Transaction transaction) {
        // 송금 받은 사람에게 알림
        createNotification(
                transaction.getToAccount().getUser(),
                Notification.NotificationType.TRANSFER_RECEIVED,
                "송금을 받았습니다",
                String.format("%s님으로부터 %s원을 받았습니다.",
                        transaction.getFromAccount().getUser().getName(),
                        formatAmount(transaction.getAmount())),
                transaction.getId(),
                transaction.getAmount()
        );

        // 송금 보낸 사람에게 알림
        createNotification(
                transaction.getFromAccount().getUser(),
                Notification.NotificationType.TRANSFER_SENT,
                "송금이 완료되었습니다",
                String.format("%s님에게 %s원 송금이 완료되었습니다.",
                        transaction.getToAccount().getUser().getName(),
                        formatAmount(transaction.getAmount())),
                transaction.getId(),
                transaction.getAmount()
        );
    }

    @Transactional
    public void createQrPaymentNotification(Transaction transaction) {
        // QR 결제 받은 사람 (판매자)
        createNotification(
                transaction.getToAccount().getUser(),
                Notification.NotificationType.QR_PAYMENT_RECEIVED,
                "QR 결제를 받았습니다",
                String.format("%s님으로부터 %s원 QR 결제를 받았습니다.",
                        transaction.getFromAccount().getUser().getName(),
                        formatAmount(transaction.getAmount())),
                transaction.getId(),
                transaction.getAmount()
        );

        // QR 결제 보낸 사람 (구매자)
        createNotification(
                transaction.getFromAccount().getUser(),
                Notification.NotificationType.QR_PAYMENT_SENT,
                "QR 결제가 완료되었습니다",
                String.format("%s님에게 %s원 QR 결제가 완료되었습니다.",
                        transaction.getToAccount().getUser().getName(),
                        formatAmount(transaction.getAmount())),
                transaction.getId(),
                transaction.getAmount()
        );
    }

    @Transactional
    public void createScheduledTransferExecutedNotification(User user, Transaction transaction, String description) {
        createNotification(
                user,
                Notification.NotificationType.SCHEDULED_TRANSFER_EXECUTED,
                "예약 송금이 실행되었습니다",
                String.format("예약된 송금 %s원이 실행되었습니다.", formatAmount(transaction.getAmount())),
                transaction.getId(),
                transaction.getAmount()
        );
    }

    @Transactional
    public void createScheduledTransferFailedNotification(User user, BigDecimal amount, String reason) {
        createNotification(
                user,
                Notification.NotificationType.SCHEDULED_TRANSFER_FAILED,
                "예약 송금 실행이 실패했습니다",
                String.format("예약된 송금 %s원 실행이 실패했습니다. 사유: %s",
                        formatAmount(amount), reason),
                null,
                amount
        );
    }

    @Transactional
    public void createLargeAmountAlertNotification(User user, BigDecimal amount, String transactionType) {
        createNotification(
                user,
                Notification.NotificationType.LARGE_AMOUNT_ALERT,
                "고액 거래 알림",
                String.format("고액 거래(%s)가 발생했습니다: %s원", transactionType, formatAmount(amount)),
                null,
                amount
        );
    }

    @Transactional
    protected void createNotification(User user, Notification.NotificationType type,
                                      String title, String content,
                                      Long transactionId, BigDecimal amount) {
        // 알림 생성
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .relatedTransactionId(transactionId)
                .amount(amount)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("알림 생성: userId={}, type={}", user.getId(), type);

        // 알림 설정 확인 후 이메일 전송
        NotificationSetting setting = notificationSettingRepository.findByUserId(user.getId())
                .orElse(createDefaultSetting(user));

        if (setting.shouldNotify(type)) {
            sendEmailNotification(user, type, title, content, amount);
        }

        // 고액 거래 알림
        if (setting.isLargeAmount(amount) &&
                (type == Notification.NotificationType.TRANSFER_SENT ||
                        type == Notification.NotificationType.QR_PAYMENT_SENT)) {
            emailService.sendLargeAmountAlertEmail(user.getEmail(), amount, type.name());
        }
    }

    private void sendEmailNotification(User user, Notification.NotificationType type,
                                       String title, String content, BigDecimal amount) {
        switch (type) {
            case TRANSFER_RECEIVED -> emailService.sendNotificationEmail(user.getEmail(), title, content);
            case TRANSFER_SENT -> emailService.sendNotificationEmail(user.getEmail(), title, content);
            case SCHEDULED_TRANSFER_EXECUTED -> emailService.sendNotificationEmail(user.getEmail(), title, content);
            case SCHEDULED_TRANSFER_FAILED -> emailService.sendNotificationEmail(user.getEmail(), title, content);
            case QR_PAYMENT_RECEIVED, QR_PAYMENT_SENT ->
                    emailService.sendNotificationEmail(user.getEmail(), title, content);
            default -> log.debug("이메일 전송 불필요: type={}", type);
        }
    }

    @Transactional
    public NotificationSetting createDefaultSetting(User user) {
        NotificationSetting setting = NotificationSetting.builder()
                .user(user)
                .emailEnabled(true)
                .transferReceivedEmail(true)
                .transferSentEmail(false)
                .scheduledTransferEmail(true)
                .qrPaymentEmail(true)
                .largeAmountAlertEnabled(true)
                .largeAmountThreshold(new BigDecimal("500000"))
                .build();

        return notificationSettingRepository.save(setting);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<Notification> notifications = notificationRepository.findByUserId(user.getId(), pageable);
        return notifications.map(NotificationResponse::of);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUnreadNotifications(Pageable pageable) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<Notification> notifications = notificationRepository.findUnreadByUserId(user.getId(), pageable);
        return notifications.map(NotificationResponse::of);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(
                user.getId(), Pageable.unpaged());

        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%,d", amount.longValue());
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
