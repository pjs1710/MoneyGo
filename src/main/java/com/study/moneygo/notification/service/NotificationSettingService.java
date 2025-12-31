package com.study.moneygo.notification.service;

import com.study.moneygo.notification.dto.request.NotificationSettingUpdateRequest;
import com.study.moneygo.notification.dto.response.NotificationSettingResponse;
import com.study.moneygo.notification.entity.NotificationSetting;
import com.study.moneygo.notification.repository.NotificationSettingRepository;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationSettingResponse getMySetting() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        NotificationSetting setting = notificationSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultSetting(user));

        return NotificationSettingResponse.of(setting);
    }

    @Transactional
    public NotificationSettingResponse updateMySetting(NotificationSettingUpdateRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        NotificationSetting setting = notificationSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultSetting(user));

        // 업데이트
        if (request.getEmailEnabled() != null) {
            setting.updateEmailEnabled(request.getEmailEnabled());
        }
        if (request.getTransferReceivedEmail() != null) {
            setting.updateTransferReceivedEmail(request.getTransferReceivedEmail());
        }
        if (request.getTransferSentEmail() != null) {
            setting.updateTransferSentEmail(request.getTransferSentEmail());
        }
        if (request.getScheduledTransferEmail() != null) {
            setting.updateScheduledTransferEmail(request.getScheduledTransferEmail());
        }
        if (request.getQrPaymentEmail() != null) {
            setting.updateQrPaymentEmail(request.getQrPaymentEmail());
        }
        if (request.getLargeAmountAlertEnabled() != null || request.getLargeAmountThreshold() != null) {
            setting.updateLargeAmountAlert(
                    request.getLargeAmountAlertEnabled() != null ?
                            request.getLargeAmountAlertEnabled() : setting.isLargeAmountAlertEnabled(),
                    request.getLargeAmountThreshold()
            );
        }

        NotificationSetting updatedSetting = notificationSettingRepository.save(setting);
        log.info("알림 설정 업데이트: userId={}", user.getId());

        return NotificationSettingResponse.of(updatedSetting);
    }

    @Transactional
    protected NotificationSetting createDefaultSetting(User user) {
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

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
