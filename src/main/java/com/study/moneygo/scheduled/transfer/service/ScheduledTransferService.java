package com.study.moneygo.scheduled.transfer.service;

import com.study.moneygo.account.entity.Account;
import com.study.moneygo.account.entity.Transaction;
import com.study.moneygo.account.entity.TransferLimit;
import com.study.moneygo.account.repository.AccountRepository;
import com.study.moneygo.account.repository.TransactionRepository;
import com.study.moneygo.account.repository.TransferLimitRepository;
import com.study.moneygo.notification.service.NotificationService;
import com.study.moneygo.scheduled.transfer.dto.request.ScheduledTransferRequest;
import com.study.moneygo.scheduled.transfer.dto.response.ScheduledTransferResponse;
import com.study.moneygo.scheduled.transfer.entity.ScheduledTransfer;
import com.study.moneygo.scheduled.transfer.repository.ScheduledTransferRepository;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTransferService {

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferLimitRepository transferLimitRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional
    public ScheduledTransferResponse createSchedule(ScheduledTransferRequest request) {
        // 현재 사용자 조회
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Account fromAccount = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        // 받는 계좌 존재 확인
        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("받는 계좌를 찾을 수 없습니다."));

        // 본인 계좌로 예약 방지
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("본인 계좌로는 송금 예약을 할 수 없습니다.");
        }

        // 예약 시간 검증 (최소 1분 후, 최대 1년 후)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxScheduleTime = now.plusYears(1);
        if (request.getScheduledAt().isBefore(now.plusMinutes(1))) {
            throw new IllegalArgumentException("예약 시간은 현재 시간으로부터 최소 1분 이후여야 합니다.");
        }
        if (request.getScheduledAt().isAfter(maxScheduleTime)) {
            throw new IllegalArgumentException("예약 시간은 1년 이내여야 합니다.");
        }

        // 송금 예약 생성
        ScheduledTransfer schedule = ScheduledTransfer.builder()
                .fromAccount(fromAccount)
                .toAccountNumber(request.getToAccountNumber())
                .amount(request.getAmount())
                .description(request.getDescription())
                .scheduledAt(request.getScheduledAt())
                .status(ScheduledTransfer.ScheduleStatus.PENDING)
                .build();

        ScheduledTransfer savedSchedule = scheduledTransferRepository.save(schedule);

        log.info("송금 예약 생성: scheduleId={}, fromAccount={}, toAccount={}, amount={}, scheduledAt={}",
                savedSchedule.getId(), fromAccount.getAccountNumber(),
                request.getToAccountNumber(), request.getAmount(), request.getScheduledAt());

        return ScheduledTransferResponse.of(savedSchedule);
    }

    @Transactional(readOnly = true)
    public Page<ScheduledTransferResponse> getMySchedules(Pageable pageable) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<ScheduledTransfer> schedules = scheduledTransferRepository.findByUserId(user.getId(), pageable);
        return schedules.map(ScheduledTransferResponse::of);
    }

    @Transactional(readOnly = true)
    public ScheduledTransferResponse getScheduleDetail(Long scheduleId) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ScheduledTransfer schedule = scheduledTransferRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 소유권 확인
        if (!schedule.getFromAccount().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return ScheduledTransferResponse.of(schedule);
    }

    @Transactional
    public void cancelSchedule(Long scheduleId) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ScheduledTransfer schedule = scheduledTransferRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 소유권 확인
        if (!schedule.getFromAccount().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        schedule.cancel();
        scheduledTransferRepository.save(schedule);

        log.info("송금 예약 취소: scheduleId={}", scheduleId);
    }

    @Transactional
    public void executeScheduledTransfer(ScheduledTransfer schedule) {
        try {
            // 계좌 조회 및 락 획득
            Account fromAccount = accountRepository.findByIdForUpdate(schedule.getFromAccount().getId())
                    .orElseThrow(() -> new IllegalStateException("송금 계좌를 찾을 수 없습니다."));

            Account toAccount = accountRepository.findByAccountNumberForUpdate(schedule.getToAccountNumber())
                    .orElseThrow(() -> new IllegalStateException("받는 계좌를 찾을 수 없습니다."));

            // 계좌 상태 확인
            if (!fromAccount.isActive() || !toAccount.isActive()) {
                throw new IllegalStateException("계좌가 활성 상태가 아닙니다.");
            }

            // 잔액 확인
            if (!fromAccount.hasEnoughBalance(schedule.getAmount())) {
                throw new IllegalArgumentException("잔액이 부족합니다.");
            }

            // 한도 확인
            TransferLimit limit = transferLimitRepository.findByAccountId(fromAccount.getId())
                    .orElse(null);
            if (limit != null) {
                limit.resetIfNewDay();
                if (!limit.canTransfer(schedule.getAmount())) {
                    throw new IllegalArgumentException("송금 한도를 초과했습니다.");
                }
            }

            // 거래 생성
            Transaction transaction = Transaction.builder()
                    .fromAccount(fromAccount)
                    .toAccount(toAccount)
                    .amount(schedule.getAmount())
                    .type(Transaction.TransactionType.TRANSFER)
                    .status(Transaction.TransactionStatus.PENDING)
                    .description(schedule.getDescription() != null
                            ? "[예약송금] " + schedule.getDescription()
                            : "[예약송금]")
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();

            // 송금 실행
            fromAccount.withdraw(schedule.getAmount());
            toAccount.deposit(schedule.getAmount());
            transaction.complete();

            if (limit != null) {
                limit.addUsage(schedule.getAmount());
                transferLimitRepository.save(limit);
            }

            transactionRepository.save(transaction);
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            // 예약 완료 처리
            schedule.execute(transaction);
            scheduledTransferRepository.save(schedule);

            log.info("예약 송금 실행 완료: scheduleId={}, transactionId={}", schedule.getId(), transaction.getId());

            // 알림 생성 (별도 트랜잭션으로 실행 - 실패해도 송금은 완료)
            Long userId = fromAccount.getUser().getId();
            Long transactionId = transaction.getId();
            String description = schedule.getDescription();
            // 트랜잭션 커밋 후 실행되도록 나중에 호출
            sendScheduledTransferNotificationAsync(userId, transactionId, schedule.getAmount(), description, true);

        } catch (Exception e) {
            // 실행 실패 처리
            schedule.fail(e.getMessage());
            scheduledTransferRepository.save(schedule);

            log.error("예약 송금 실행 실패: scheduleId={}, error={}", schedule.getId(), e.getMessage());

            // 실패 알림 생성 (별도 트랜잭션)
            Long userId = schedule.getFromAccount().getUser().getId();
            sendScheduledTransferNotificationAsync(userId, null, schedule.getAmount(), e.getMessage(), false);
        }
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendScheduledTransferNotificationAsync(Long userId, Long transactionId,
                                                       java.math.BigDecimal amount,
                                                       String info, boolean isSuccess) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

            if (isSuccess) {
                Transaction transaction = transactionRepository.findById(transactionId)
                        .orElseThrow(() -> new IllegalStateException("거래를 찾을 수 없습니다."));
                notificationService.createScheduledTransferExecutedNotification(user, transaction, info);
            } else {
                notificationService.createScheduledTransferFailedNotification(user, amount, info);
            }
        } catch (Exception e) {
            log.error("알림 전송 실패: userId={}, isSuccess={}, error={}", userId, isSuccess, e.getMessage());
        }
    }
}
