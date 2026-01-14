package com.study.moneygo.account.service;

import com.study.moneygo.account.dto.response.AccountOwnerResponse;
import com.study.moneygo.account.dto.response.AccountResponse;
import com.study.moneygo.account.entity.Account;
import com.study.moneygo.transaction.entity.Transaction;
import com.study.moneygo.account.repository.AccountRepository;
import com.study.moneygo.transaction.repository.TransactionRepository;
import com.study.moneygo.deposit.dto.request.SelfDepositRequest;
import com.study.moneygo.deposit.dto.response.SelfDepositResponse;
import com.study.moneygo.notification.service.NotificationService;
import com.study.moneygo.simplepassword.service.SimplePasswordService;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SimplePasswordService simplePasswordService;
    private final NotificationService notificationService;

    public AccountResponse getMyAccount() {
        String email = getCurrentUserEmail();
        System.out.println("======현재 인증된 이메일 : " + email + " =======");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        return AccountResponse.of(account);
    }

    public AccountOwnerResponse getAccountOwner(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계좌번호입니다."));
        return AccountOwnerResponse.of(account.getAccountNumber(), account.getUser().getName());
    }

    @Transactional
    public SelfDepositResponse selfDeposit(SelfDepositRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        simplePasswordService.verifySimplePasswordForUser(user.getId(), request.getSimplePassword());
        Account account = accountRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        // 계정 활성 상태 확인
        if (!account.isActive()) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }
        // 입금 처리
        account.deposit(request.getAmount());

        // 거래 내역 생성 (본인 입금은 fromAccount 존재 X)
        Transaction transaction = Transaction.builder()
                .fromAccount(null) // 본인 입금은 송금자가 없음
                .toAccount(account)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.PENDING)
                .description(request.getDescription() != null && !request.getDescription().isBlank()
                        ? "[본인입금] " + request.getDescription()
                        : "[본인입금]")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        transaction.complete();

        transactionRepository.save(transaction);
        accountRepository.save(account);

        log.info("본인 계좌 입금 완료: userId={}, accountNumber={}, amount={}, balanceAfter={}",
                user.getId(), account.getAccountNumber(), request.getAmount(), account.getBalance());
        try {
            notificationService.createSelfDepositNotification(user, transaction);
        } catch (Exception e) {
            log.error("본인 입금 알림 생성 실패: userId={}, error={}", user.getId(), e.getMessage());
        }

        return SelfDepositResponse.of(transaction, account.getBalance());
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        System.out.println("===== Authentication Name: " + authentication.getName() + " =====");  // 디버깅
        System.out.println("===== Authentication Principal: " + authentication.getPrincipal() + " =====");  // 디버깅

        return authentication.getName();
    }
}
