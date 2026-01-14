package com.study.moneygo.qrpayment.service;

import com.study.moneygo.account.entity.Account;
import com.study.moneygo.transaction.entity.Transaction;
import com.study.moneygo.account.repository.AccountRepository;
import com.study.moneygo.transaction.repository.TransactionRepository;
import com.study.moneygo.notification.service.NotificationService;
import com.study.moneygo.qrpayment.dto.request.QrGenerateRequest;
import com.study.moneygo.qrpayment.dto.request.QrPayRequest;
import com.study.moneygo.qrpayment.dto.response.QrGenerateResponse;
import com.study.moneygo.qrpayment.dto.response.QrPayResponse;
import com.study.moneygo.qrpayment.entity.QrPayment;
import com.study.moneygo.qrpayment.repository.QrPaymentRepository;
import com.study.moneygo.simplepassword.service.SimplePasswordService;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrPaymentService {

    private final QrPaymentRepository qrPaymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SimplePasswordService simplePasswordService;
    private final NotificationService notificationService;

    private static final int QR_EXPIRATION_MINUTES = 10; // QR 유효시간 10분

    @Transactional
    public QrGenerateResponse generateQrCode(QrGenerateRequest request) {
        // 현재 로그인한 사용자 (판매자)
        String email = getCurrentUserEmail();
        User seller = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Account sellerAccount = accountRepository.findByUserId(seller.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        // QR코드 생성 (고유값)
        String qrCode = generateUniqueQrCode();

        // 만료 시간 설정 (현재 시간 + 10분)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(QR_EXPIRATION_MINUTES);

        // QR결제 정보 생성
        QrPayment qrPayment = QrPayment.builder()
                .sellerAccount(sellerAccount)
                .amount(request.getAmount())
                .qrCode(qrCode)
                .status(QrPayment.QrPaymentStatus.PENDING)
                .description(request.getDescription())
                .expiresAt(expiresAt)
                .build();
        QrPayment savedQrPayment = qrPaymentRepository.save(qrPayment);

        log.info("QR 코드 생성 완료: qrCode={}, sellerId={}", qrCode, seller.getId());
        return QrGenerateResponse.of(savedQrPayment);
    }

    @Transactional
    public QrPayResponse payWithQrCode(QrPayRequest request) {
        // 현재 로그인한 사용자 (구매자)
        String email = getCurrentUserEmail();
        User buyer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        log.info("QR 결제 시작: buyerEmail={}, qrCode={}", email, request.getQrCode());

        // QR결제 정보 조회
        QrPayment qrPayment = qrPaymentRepository.findByQrCode(request.getQrCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 QR 코드입니다."));

        // QR코드 상태 확인
        if (!qrPayment.isPending()) {
            throw new IllegalArgumentException("이미 사용되었거나 취소된 QR 코드입니다.");
        }

        // 만료 확인
        if (qrPayment.isExpired()) {
            qrPayment.expire();
            qrPaymentRepository.save(qrPayment);
            throw new IllegalArgumentException("만료된 QR 코드입니다.");
        }

        // 구매자 계좌 조회 (비관적 Lock)
        Account buyerAccount = accountRepository.findByUserIdForUpdate(buyer.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        log.info("구매자 계좌 조회 완료: buyerAccountId={}, balance={}",
                buyerAccount.getId(), buyerAccount.getBalance());

        // 판매자 계좌 조회 (비관적 Lock)
        Account sellerAccount = accountRepository.findByIdForUpdate(qrPayment.getSellerAccount().getId())
                .orElseThrow(() -> new IllegalStateException("판매자 계좌를 찾을 수 없습니다."));

        log.info("판매자 계좌 조회 완료: sellerAccountId={}", sellerAccount.getId());

        // 본인 QR코드 결제 방지
        if (buyerAccount.getId().equals(sellerAccount.getId())) {
            throw new IllegalArgumentException("본인이 생성한 QR 코드는 결제할 수 없습니다.");
        }

        // 간편 비밀번호 확인
        log.info("간편 비밀번호 확인 시작: buyerId={}", buyer.getId());
        simplePasswordService.verifySimplePasswordForUser(buyer.getId(), request.getSimplePassword());
        log.info("간편 비밀번호 확인 완료");

        // 계좌 상태 확인
        if (!buyerAccount.isActive() || !sellerAccount.isActive()) {
            throw new IllegalStateException("계좌가 활성 상태가 아닙니다.");
        }

        // 잔액 확인
        if (!buyerAccount.hasEnoughBalance(qrPayment.getAmount())) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        // 거래 내역 생성
        Transaction transaction = Transaction.builder()
                .fromAccount(buyerAccount)
                .toAccount(sellerAccount)
                .amount(qrPayment.getAmount())
                .type(Transaction.TransactionType.QR_PAYMENT)
                .status(Transaction.TransactionStatus.PENDING)
                .description(qrPayment.getDescription() != null ? "QR결제 : " + qrPayment.getDescription() : "QR결제")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        try {
            log.info("QR 결제 실행 시작: amount={}", qrPayment.getAmount());

            // 결제 실행
            buyerAccount.withdraw(qrPayment.getAmount());
            log.info("구매자 출금 완료: newBalance={}", buyerAccount.getBalance());

            sellerAccount.deposit(qrPayment.getAmount());
            log.info("판매자 입금 완료: newBalance={}", sellerAccount.getBalance());

            // 거래 완료
            transaction.complete();
            qrPayment.complete(transaction);

            // 저장
            transactionRepository.save(transaction);
            accountRepository.save(buyerAccount);
            accountRepository.save(sellerAccount);
            qrPaymentRepository.save(qrPayment);

            log.info("QR 결제 저장 완료: transactionId={}", transaction.getId());

            // 알림 생성 (실패해도 결제는 완료됨)
            try {
                log.info("알림 생성 시작");
                notificationService.createQrPaymentNotification(transaction);
                log.info("알림 생성 완료");
            } catch (Exception notificationError) {
                // 알림 실패는 로그만 남기고 결제는 정상 진행
                log.error("알림 생성 실패 (결제는 정상 완료): transactionId={}, error={}",
                        transaction.getId(), notificationError.getMessage(), notificationError);
            }

            log.info("QR 결제 완료: transactionId={}, buyerId={}, sellerId={}",
                    transaction.getId(), buyer.getId(), sellerAccount.getUser().getId());

            return QrPayResponse.of(
                    qrPayment,
                    transaction,
                    sellerAccount.getUser().getName(),
                    buyerAccount.getBalance()
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 비즈니스 로직 오류 (잔액 부족, 상태 오류 등)
            log.error("QR 결제 실패 (비즈니스 로직 오류): {}", e.getMessage());
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            throw e; // 원래 예외를 그대로 던짐
        } catch (Exception e) {
            // 예상치 못한 시스템 오류
            log.error("QR 결제 실패 (시스템 오류): {}", e.getMessage(), e);
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            throw new IllegalStateException("QR 결제 처리 중 오류가 발생했습니다 : " + e.getMessage(), e);
        }
    }

    private String generateUniqueQrCode() {
        String qrCode;
        do {
            // QR_yyyyMMdd_randomUUID 형식
            qrCode = "QR_" + LocalDateTime.now().toString().substring(0, 10).replace("-", "")
                    + "_" + UUID.randomUUID().toString().substring(0, 8);
        } while (qrPaymentRepository.existsByQrCode(qrCode));
        return qrCode;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
