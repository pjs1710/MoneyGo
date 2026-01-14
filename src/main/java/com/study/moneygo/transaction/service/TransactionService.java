package com.study.moneygo.transaction.service;

import com.study.moneygo.transaction.dto.response.TransactionResponse;
import com.study.moneygo.account.entity.Account;
import com.study.moneygo.transaction.entity.Transaction;
import com.study.moneygo.account.repository.AccountRepository;
import com.study.moneygo.transaction.repository.TransactionRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public Page<TransactionResponse> getTransactions(String type, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        Page<Transaction> transactions;

        if (startDate != null && endDate != null) {
            // 날짜 범위 조회
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
            transactions = transactionRepository.findByAccountIdAndDateRange(
                    account.getId(), startDateTime, endDateTime, pageable
            );
        } else if ("SENT".equalsIgnoreCase(type)) {
            // 송금한 내역
            transactions = transactionRepository.findSentTransactions(account.getId(), pageable);
        } else if ("RECEIVED".equalsIgnoreCase(type)) {
            // 받은 내역
            transactions = transactionRepository.findReceivedTransactions(account.getId(), pageable);
        } else {
            // 전체 내역
            transactions = transactionRepository.findByAccountId(account.getId(), pageable);
        }

        return transactions.map(transaction -> {
            String countpartyName = getCounterpartyName(transaction, account.getId());
            return TransactionResponse.of(transaction, account.getId(), countpartyName);
        });
    }

    public TransactionResponse getTransactionDetail(Long transactionId) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래 내역을 찾을 수 없습니다."));

        // 본인의 거래인지 확인
        boolean isMyTransaction = false;
        if (transaction.getFromAccount() != null && transaction.getFromAccount().getId().equals(account.getId())) {
            isMyTransaction = true;
        }
        if (transaction.getToAccount() != null && transaction.getToAccount().getId().equals(account.getId())) {
            isMyTransaction = true;
        }

        if (!isMyTransaction) {
            throw new IllegalArgumentException("본인의 거래 내역만 조회할 수 있습니다.");
        }

        String counterpartyName = getCounterpartyName(transaction, account.getId());
        return TransactionResponse.of(transaction, account.getId(), counterpartyName);
    }

    public Page<TransactionResponse> getFilteredTransactions(
            LocalDate startDate,
            LocalDate endDate,
            String type,
            Pageable pageable
    ) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        // 날짜 범위 기본값 설정
        LocalDateTime startDateTime = startDate != null
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusYears(1); // default 1년
        LocalDateTime endDateTime = endDate != null
                ? endDate.atTime(23, 59, 59)
                : LocalDateTime.now();
        Page<Transaction> transactions;

        if (type != null && !type.isEmpty()) {
            // 거래 유형별 필터 적용
            try {
                Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(type.toUpperCase());
                transactions = transactionRepository.findByAccountAndTypeAndDateRange(
                        account.getId(), transactionType, startDateTime, endDateTime, pageable
                );
            } catch (Exception e) {
                log.warn("잘못된 거래 유형: {}", type);
                // 잘못된 타입이면 전체 조회
                transactions = transactionRepository.findByAccountAndDateRange(
                        account.getId(), startDateTime, endDateTime, pageable
                );
            }
        } else {
            // 거래 유형이 아니면 날짜 필터 적용
            transactions = transactionRepository.findByAccountAndDateRange(
                    account.getId(), startDateTime, endDateTime, pageable
            );
        }
        return transactions.map(transaction -> {
            String counterpartyName = null;
            if (transaction.getFromAccount() != null &&
                    !transaction.getFromAccount().getId().equals(account.getId())) {
                counterpartyName = transaction.getFromAccount().getUser().getName();
            } else if (transaction.getToAccount() != null &&
                    !transaction.getToAccount().getId().equals(account.getId())) {
                counterpartyName = transaction.getToAccount().getUser().getName();
            }

            return TransactionResponse.of(transaction, account.getId(), counterpartyName);
        });
    }

    private String getCounterpartyName(Transaction transaction, Long myAccountId) {
        if (transaction.getFromAccount() != null &&
                transaction.getFromAccount().getId().equals(myAccountId)) {
            // 내가 보낸 거래
            return transaction.getToAccount() != null ?
                    transaction.getToAccount().getUser().getName() : "시스템";
        } else if (transaction.getToAccount() != null &&
                transaction.getToAccount().getId().equals(myAccountId)) {
            // 내가 받은 거래
            return transaction.getFromAccount() != null ?
                    transaction.getFromAccount().getUser().getName() : "시스템";
        }
        return "알 수 없음";
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}

