package com.study.moneygo.admin.service;

import com.study.moneygo.account.entity.Account;
import com.study.moneygo.account.entity.Transaction;
import com.study.moneygo.account.repository.AccountRepository;
import com.study.moneygo.account.repository.TransactionRepository;
import com.study.moneygo.admin.dto.request.AdminDepositRequest;
import com.study.moneygo.admin.dto.request.AdminWithdrawRequest;
import com.study.moneygo.admin.dto.response.AdminDepositResponse;
import com.study.moneygo.admin.dto.response.AdminWithdrawResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public AdminDepositResponse deposit(Long accountId, AdminDepositRequest request) {
        try {
            System.out.println("===== 충전 시작 : accountId = " + accountId + ", amount = " + request.getAmount() + " =====");

            // 계좌 조회 (비관적 Lock)
            Account account = accountRepository.findByIdForUpdate(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
            System.out.println("===== 계좌 조회 성공 : " + account.getAccountNumber() + " =====");
            BigDecimal balanceBefore = account.getBalance();

            // 거래 내역 생성
            Transaction transaction = Transaction.builder()
                    .toAccount(account)
                    .amount(request.getAmount())
                    .type(Transaction.TransactionType.DEPOSIT)
                    .status(Transaction.TransactionStatus.PENDING)
                    .description(request.getDescription() != null ? request.getDescription() : "관리자 충전")
                    .build();
            System.out.println("===== 거래 내역 생성 성공 =====");

            // 충전 실행
            account.deposit(request.getAmount());
            transaction.complete();
            System.out.println("===== 충전 실행 성공 =====");

            // 저장
            accountRepository.save(account);
            transactionRepository.save(transaction);
            System.out.println("===== 저장 완료 =====");

            return AdminDepositResponse.of(
                    account.getAccountNumber(),
                    balanceBefore,
                    account.getBalance(),
                    request.getAmount()
            );
        } catch (Exception e) {
            System.err.println("===== 충전 중 오류 발생 =====");
            e.printStackTrace();
            throw new IllegalStateException("충전 처리 중 오류가 발생했습니다 : " + e.getMessage());
        }
    }

    @Transactional
    public AdminWithdrawResponse withdraw(Long accountId, AdminWithdrawRequest request) {
        // 계좌 조회 (비관적 Lock)
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        BigDecimal balanceBefore = account.getBalance();

        // 잔액 확인
        if (!account.hasEnoughBalance(request.getAmount())) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        // 거래 내역 생성
        Transaction transaction = Transaction.builder()
                .fromAccount(account)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.WITHDRAW)
                .status(Transaction.TransactionStatus.PENDING)
                .description(request.getDescription() != null ? request.getDescription() : "관리자 인출")
                .build();

        try {
            // 인출 실행
            account.withdraw(request.getAmount());
            transaction.complete();

            // 저장
            accountRepository.save(account);
            transactionRepository.save(transaction);

            return AdminWithdrawResponse.of(
                    account.getAccountNumber(),
                    balanceBefore,
                    account.getBalance(),
                    request.getAmount()
            );
        } catch (Exception e) {
            transaction.fail(e.getMessage());
            transactionRepository.save(transaction);

            throw new IllegalStateException("인출 처리 중 오류가 발생했습니다 : " + e.getMessage());
        }
    }
}
