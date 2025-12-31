package com.study.moneygo.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendNotificationEmail(String to, String title, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@moneygo.com");
            message.setTo(to);
            message.setSubject("[MoneyGo] " + title);
            message.setText(content);

            mailSender.send(message);
            log.info("이메일 전송 완료: to={}, title={}", to, title);
        } catch (Exception e) {
            log.error("이메일 전송 실패: to={}, error={}", to, e.getMessage(), e);
        }
    }

    public void sendTransferReceivedEmail(String to, String senderName, BigDecimal amount) {
        String title = "송금을 받았습니다";
        String content = String.format(
                """
                안녕하세요,
                
                %s님으로부터 %s원을 받았습니다.
                
                거래 내역은 MoneyGo 앱에서 확인하실 수 있습니다.
                
                감사합니다.
                
                MoneyGo
                """,
                senderName,
                formatCurrency(amount)
        );

        sendNotificationEmail(to, title, content);
    }

    public void sendTransferSentEmail(String to, String receiverName, BigDecimal amount) {
        String title = "송금이 완료되었습니다";
        String content = String.format(
                """
                안녕하세요,
                
                %s님에게 %s원 송금이 완료되었습니다.
                
                거래 내역은 MoneyGo 앱에서 확인하실 수 있습니다.
                
                감사합니다.
                
                MoneyGo
                """,
                receiverName,
                formatCurrency(amount)
        );

        sendNotificationEmail(to, title, content);
    }

    public void sendScheduledTransferExecutedEmail(String to, BigDecimal amount, String description) {
        String title = "예약 송금이 실행되었습니다";
        String content = String.format(
                """
                안녕하세요,
                
                예약된 송금이 실행되었습니다.
                
                금액: %s원
                내용: %s
                
                거래 내역은 MoneyGo 앱에서 확인하실 수 있습니다.
                
                감사합니다.
                
                MoneyGo
                """,
                formatCurrency(amount),
                description != null ? description : "없음"
        );

        sendNotificationEmail(to, title, content);
    }

    public void sendScheduledTransferFailedEmail(String to, BigDecimal amount, String reason) {
        String title = "예약 송금 실행이 실패했습니다";
        String content = String.format(
                """
                안녕하세요,
                
                예약된 송금 실행이 실패했습니다.
                
                금액: %s원
                실패 사유: %s
                
                계좌 잔액을 확인하시고 다시 시도해주세요.
                
                감사합니다.
                
                MoneyGo
                """,
                formatCurrency(amount),
                reason
        );

        sendNotificationEmail(to, title, content);
    }

    public void sendQrPaymentEmail(String to, String counterpartyName, BigDecimal amount, boolean isReceived) {
        String title = isReceived ? "QR 결제를 받았습니다" : "QR 결제가 완료되었습니다";
        String content = String.format(
                """
                안녕하세요,
                
                %s님과의 QR 결제가 완료되었습니다.
                
                금액: %s원
                
                거래 내역은 MoneyGo 앱에서 확인하실 수 있습니다.
                
                감사합니다.
                
                MoneyGo
                """,
                counterpartyName,
                formatCurrency(amount)
        );

        sendNotificationEmail(to, title, content);
    }

    public void sendLargeAmountAlertEmail(String to, BigDecimal amount, String transactionType) {
        String title = "고액 거래 알림";
        String content = String.format(
                """
                안녕하세요,
                
                고액 거래가 발생했습니다.
                
                거래 유형: %s
                금액: %s원
                
                본인의 거래가 아니라면 즉시 고객센터로 연락해주세요.
                
                감사합니다.
                
                MoneyGo
                """,
                transactionType,
                formatCurrency(amount)
        );

        sendNotificationEmail(to, title, content);
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
        return formatter.format(amount);
    }
}
