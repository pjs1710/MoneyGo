package com.study.moneygo.transfer.service;

import com.study.moneygo.transfer.dto.request.TransferRequest;
import com.study.moneygo.transfer.dto.response.TransferResponse;
import com.study.moneygo.account.entity.Account;
import com.study.moneygo.account.entity.Transaction;
import com.study.moneygo.transfer.entity.TransferLimit;
import com.study.moneygo.account.repository.AccountRepository;
import com.study.moneygo.account.repository.TransactionRepository;
import com.study.moneygo.transfer.repository.TransferLimitRepository;
import com.study.moneygo.notification.service.NotificationService;
import com.study.moneygo.simplepassword.service.SimplePasswordService;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferLimitRepository transferLimitRepository;
    private final UserRepository userRepository;
    private final SimplePasswordService simplePasswordService;
    private final NotificationService notificationService;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        // 1. 현재 로그인한 사용자 확인하기
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 보내는 계좌 조회 (비관적 Lock)
        Account fromAccount = accountRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        // 3. 받는 계좌 조회 (비관적 Lock)
        Account toAccount = accountRepository.findByAccountNumberForUpdate(request.getToAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("받는 계좌를 찾을 수 없습니다."));

        // 4. 본인 계좌로 송금 방지
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("본인 계좌로는 송금할 수 없습니다.");
        }

        // 5. 간편 비밀번호 확인
        simplePasswordService.verifySimplePasswordForUser(user.getId(), request.getSimplePassword());

        // 6. 계좌 상태 확인
        if (!fromAccount.isActive()) {
            throw new IllegalStateException("송금 가능한 계좌 상태가 아닙니다.");
        }
        if (!toAccount.isActive()) {
            throw new IllegalArgumentException("받는 계좌가 활성 상태가 아닙니다.");
        }

        // 7. 잔액 확인
        if (!fromAccount.hasEnoughBalance(request.getAmount())) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        // 8. 송금 한도 확인
        TransferLimit transferLimit = transferLimitRepository.findByAccountIdForUpdate(fromAccount.getId())
                .orElseGet(() -> createDefaultTransferLimit(fromAccount));

        if (!transferLimit.canTransfer(request.getAmount())) {
            throw new IllegalArgumentException(
                    String.format("송금 한도를 초과했습니다. (1회 한도: %s원, 일일 한도: %s원, 남은 한도: %s원)",
                            transferLimit.getPerTransactionLimit(),
                            transferLimit.getDailyLimit(),
                            transferLimit.getRemainingDailyLimit())
            );
        }

        // 9. 중복 요청 방지 (Idempotency Key)
        String idempotencyKey = UUID.randomUUID().toString();

        // 10. 거래 내역 생성
        Transaction transaction = Transaction.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.PENDING)
                .description(request.getDescription())
                .idempotencyKey(idempotencyKey)
                .build();

        try {
            // 11. 송금 실행
            fromAccount.withdraw(request.getAmount());
            toAccount.deposit(request.getAmount());

            // 12. 한도 사용량 업데이트
            transferLimit.addUsage(request.getAmount());

            // 13. 거래 완료 처리
            transaction.complete();

            // 알림 생성
            notificationService.createTransferNotification(transaction);

            // 14. 저장
            transactionRepository.save(transaction);
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
            transferLimitRepository.save(transferLimit);

            return TransferResponse.of(transaction, toAccount.getUser().getName(), fromAccount.getBalance());
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);
            throw new IllegalStateException("송금 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private TransferLimit createDefaultTransferLimit(Account account) {
        TransferLimit transferLimit = TransferLimit.builder()
                .account(account)
                .dailyLimit(new BigDecimal("3000000.00"))
                .perTransactionLimit(new BigDecimal("1000000.00"))
                .todayUsed(BigDecimal.ZERO)
                .build();

        return transferLimitRepository.save(transferLimit);
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
