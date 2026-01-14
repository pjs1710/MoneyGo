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
import java.util.List;

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
        try {
            String senderName = transaction.getFromAccount().getUser().getName();
            String receiverName = transaction.getToAccount().getUser().getName();
            BigDecimal amount = transaction.getAmount();

            // 송금 받은 사람에게 알림
            createNotificationWithEmail(
                    transaction.getToAccount().getUser(),
                    Notification.NotificationType.TRANSFER_RECEIVED,
                    "송금을 받았습니다",
                    String.format("%s님으로부터 %s원을 받았습니다.", senderName, formatAmount(amount)),
                    transaction.getId(),
                    amount,
                    senderName,
                    null
            );

            // 송금 보낸 사람에게 알림
            createNotificationWithEmail(
                    transaction.getFromAccount().getUser(),
                    Notification.NotificationType.TRANSFER_SENT,
                    "송금이 완료되었습니다",
                    String.format("%s님에게 %s원 송금이 완료되었습니다.", receiverName, formatAmount(amount)),
                    transaction.getId(),
                    amount,
                    receiverName,
                    null
            );
        } catch (Exception e) {
            log.error("송금 알림 생성 중 오류 발생: transactionId={}, error={}",
                    transaction.getId(), e.getMessage(), e);
            // 알림 생성 실패해도 예외를 던지지 않음 (거래는 이미 완료됨)
        }
    }

    @Transactional
    public void createQrPaymentNotification(Transaction transaction) {
        try {
            String buyerName = transaction.getFromAccount().getUser().getName();
            String sellerName = transaction.getToAccount().getUser().getName();
            BigDecimal amount = transaction.getAmount();

            log.info("QR 결제 알림 생성 시작: buyerName={}, sellerName={}, amount={}",
                    buyerName, sellerName, amount);

            // QR 결제 받은 사람 (판매자)
            createNotificationWithEmail(
                    transaction.getToAccount().getUser(),
                    Notification.NotificationType.QR_PAYMENT_RECEIVED,
                    "QR 결제를 받았습니다",
                    String.format("%s님으로부터 %s원 QR 결제를 받았습니다.", buyerName, formatAmount(amount)),
                    transaction.getId(),
                    amount,
                    buyerName,
                    null
            );

            // QR 결제 보낸 사람 (구매자)
            createNotificationWithEmail(
                    transaction.getFromAccount().getUser(),
                    Notification.NotificationType.QR_PAYMENT_SENT,
                    "QR 결제가 완료되었습니다",
                    String.format("%s님에게 %s원 QR 결제가 완료되었습니다.", sellerName, formatAmount(amount)),
                    transaction.getId(),
                    amount,
                    sellerName,
                    null
            );

            log.info("QR 결제 알림 생성 완료");
        } catch (Exception e) {
            log.error("QR 결제 알림 생성 중 오류 발생: transactionId={}, error={}",
                    transaction.getId(), e.getMessage(), e);
            // 알림 생성 실패해도 예외를 던지지 않음 (거래는 이미 완료됨)
        }
    }

    @Transactional
    public void createScheduledTransferExecutedNotification(User user, Transaction transaction, String description) {
        try {
            createNotificationWithEmail(
                    user,
                    Notification.NotificationType.SCHEDULED_TRANSFER_EXECUTED,
                    "예약 송금이 실행되었습니다",
                    String.format("예약된 송금 %s원이 실행되었습니다.", formatAmount(transaction.getAmount())),
                    transaction.getId(),
                    transaction.getAmount(),
                    null,
                    description
            );
        } catch (Exception e) {
            log.error("예약 송금 실행 알림 생성 중 오류 발생: userId={}, error={}",
                    user.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    public void createScheduledTransferFailedNotification(User user, BigDecimal amount, String reason) {
        try {
            createNotificationWithEmail(
                    user,
                    Notification.NotificationType.SCHEDULED_TRANSFER_FAILED,
                    "예약 송금 실행이 실패했습니다",
                    String.format("예약된 송금 %s원 실행이 실패했습니다. 사유: %s", formatAmount(amount), reason),
                    null,
                    amount,
                    null,
                    reason
            );
        } catch (Exception e) {
            log.error("예약 송금 실패 알림 생성 중 오류 발생: userId={}, error={}",
                    user.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    public void createLargeAmountAlertNotification(User user, BigDecimal amount, String transactionType) {
        try {
            createNotificationWithEmail(
                    user,
                    Notification.NotificationType.LARGE_AMOUNT_ALERT,
                    "고액 거래 알림",
                    String.format("고액 거래(%s)가 발생했습니다: %s원", transactionType, formatAmount(amount)),
                    null,
                    amount,
                    null,
                    transactionType
            );
        } catch (Exception e) {
            log.error("고액 거래 알림 생성 중 오류 발생: userId={}, error={}",
                    user.getId(), e.getMessage(), e);
        }
    }

    @Transactional
    protected void createNotificationWithEmail(User user, Notification.NotificationType type,
                                               String title, String content,
                                               Long transactionId, BigDecimal amount,
                                               String counterpartyName, String additionalInfo) {
        try {
            log.info("알림 생성 시작: userId={}, type={}", user.getId(), type);

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
            log.info("알림 DB 저장 완료: userId={}, type={}", user.getId(), type);

            // 알림 설정 확인 후 이메일 전송
            try {
                NotificationSetting setting = getOrCreateNotificationSetting(user);
                log.info("알림 설정 조회 완료: userId={}, emailEnabled={}",
                        user.getId(), setting.isEmailEnabled());

                if (setting.shouldNotify(type)) {
                    log.info("이메일 발송 시작: email={}, type={}", user.getEmail(), type);
                    sendEmailNotificationWithDetails(user, type, amount, counterpartyName, additionalInfo);
                    log.info("이메일 발송 완료");
                } else {
                    log.info("알림 설정에 의해 이메일 발송 스킵: userId={}, type={}", user.getId(), type);
                }

                // 고액 거래 알림
                if (setting.isLargeAmount(amount) &&
                        (type == Notification.NotificationType.TRANSFER_SENT ||
                                type == Notification.NotificationType.QR_PAYMENT_SENT)) {
                    log.info("고액 거래 알림 이메일 발송 시작: amount={}", amount);
                    emailService.sendLargeAmountAlertEmail(user.getEmail(), amount, type.name());
                }
            } catch (Exception emailError) {
                // 이메일 전송 실패는 로그만 남기고 계속 진행
                log.error("이메일 전송 중 오류 발생했지만 알림은 생성됨: userId={}, type={}, error={}",
                        user.getId(), type, emailError.getMessage());
            }
        } catch (Exception e) {
            // 알림 생성 실패
            log.error("알림 생성 중 오류 발생: userId={}, type={}, error={}",
                    user.getId(), type, e.getMessage(), e);
            // 예외를 던지지 않음 (거래는 이미 완료됨)
        }
    }

    private void sendEmailNotificationWithDetails(User user, Notification.NotificationType type,
                                                  BigDecimal amount, String counterpartyName,
                                                  String additionalInfo) {
        try {
            switch (type) {
                case TRANSFER_RECEIVED ->
                        emailService.sendTransferReceivedEmail(user.getEmail(), counterpartyName, amount);
                case TRANSFER_SENT ->
                        emailService.sendTransferSentEmail(user.getEmail(), counterpartyName, amount);
                case SCHEDULED_TRANSFER_EXECUTED ->
                        emailService.sendScheduledTransferExecutedEmail(user.getEmail(), amount, additionalInfo);
                case SCHEDULED_TRANSFER_FAILED ->
                        emailService.sendScheduledTransferFailedEmail(user.getEmail(), amount, additionalInfo);
                case QR_PAYMENT_RECEIVED ->
                        emailService.sendQrPaymentEmail(user.getEmail(), counterpartyName, amount, true);
                case QR_PAYMENT_SENT ->
                        emailService.sendQrPaymentEmail(user.getEmail(), counterpartyName, amount, false);
                default -> log.debug("이메일 전송 불필요: type={}", type);
            }
        } catch (Exception e) {
            log.error("이메일 전송 중 오류: type={}, error={}", type, e.getMessage());
            // 예외를 던지지 않음
        }
    }

    private NotificationSetting getOrCreateNotificationSetting(User user) {
        return notificationSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    try {
                        return createDefaultSettingInNewTransaction(user);
                    } catch (Exception e) {
                        log.warn("알림 설정 생성 중 오류 발생 (중복 가능성), 재조회: userId={}", user.getId());
                        return notificationSettingRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new IllegalStateException("알림 설정을 생성할 수 없습니다."));
                    }
                });
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public NotificationSetting createDefaultSettingInNewTransaction(User user) {
        return notificationSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> {
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
                });
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

    /*
    알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 소유권 확인
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        notificationRepository.delete(notification);
        log.info("알림 삭제 완료: notificationId={}, userId={}", notificationId, user.getId());
    }

    /*
    읽은 알림 전체 삭제
     */
    @Transactional
    public void deleteAllReadNotifications() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Notification> readNotifications = notificationRepository.findByUserIdAndIsReadTrue(user.getId());
        notificationRepository.deleteAll(readNotifications);
        log.info("읽은 알림 전체 삭제 완료: userId={}, count={}", user.getId(), readNotifications.size());
    }

    /*
    내 계좌에 입금
     */
    @Transactional
    public void createSelfDepositNotification(User user, Transaction transaction) {
        try {
            createNotificationWithEmail(
                    user,
                    Notification.NotificationType.SELF_DEPOSIT,
                    "계좌 입금이 완료되었습니다.",
                    String.format("내 계좌에 %s원이 입금되었습니다.", formatAmount(transaction.getAmount())),
                    transaction.getId(),
                    transaction.getAmount(),
                    null,
                    null
            );
        } catch (Exception e) {
            log.error("본인 입금 알림 생성 중 오류 발생: userId={}, error={}",
                    user.getId(), e.getMessage(), e);
        }
    }


    private String formatAmount(BigDecimal amount) {
        return String.format("%,d", amount.longValue());
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
