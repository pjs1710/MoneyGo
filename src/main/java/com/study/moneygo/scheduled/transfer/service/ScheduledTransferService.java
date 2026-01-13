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
import com.study.moneygo.simplepassword.service.SimplePasswordService;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final SimplePasswordService simplePasswordService;
    private final NotificationService notificationService;

    @Transactional
    public ScheduledTransferResponse createSchedule(ScheduledTransferRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 계좌 조회 (락 획득)
        Account fromAccount = accountRepository.findByIdForUpdate(
                accountRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."))
                        .getId()
        ).orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

        // 간편 비밀번호 확인
        simplePasswordService.verifySimplePasswordForUser(user.getId(), request.getPassword());

        // 받는 계좌 존재 확인
        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("받는 계좌를 찾을 수 없습니다."));

        // 본인 계좌로 예약 방지
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("본인 계좌로는 송금 예약을 할 수 없습니다.");
        }

        // 잔액 확인 및 즉시 차감
        if (!fromAccount.hasEnoughBalance(request.getAmount())) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        fromAccount.withdraw(request.getAmount());
        accountRepository.save(fromAccount);

        // 예약 시간 검증
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

        log.info("송금 예약 생성 및 잔액 차감: scheduleId={}, fromAccount={}, amount={}, balanceAfter={}",
                savedSchedule.getId(), fromAccount.getAccountNumber(),
                request.getAmount(), fromAccount.getBalance());

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

        // 취소 시 잔액 환불
        if (schedule.getStatus() == ScheduledTransfer.ScheduleStatus.PENDING) {
            Account fromAccount = accountRepository.findByIdForUpdate(schedule.getFromAccount().getId())
                    .orElseThrow(() -> new IllegalStateException("계좌를 찾을 수 없습니다."));

            fromAccount.deposit(schedule.getAmount());
            accountRepository.save(fromAccount);

            log.info("예약 취소 및 잔액 환불: scheduleId={}, amount={}, balanceAfter={}",
                    scheduleId, schedule.getAmount(), fromAccount.getBalance());
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
                // 실패 시 잔액 환불
                fromAccount.deposit(schedule.getAmount());
                accountRepository.save(fromAccount);
                throw new IllegalStateException("계좌가 활성 상태가 아닙니다.");
            }

            // 이미 차감되어 있으므로 잔액 확인 불필요
            // 단, 안전을 위해 확인은 해도 됨
            if (!fromAccount.hasEnoughBalance(BigDecimal.ZERO)) {
                // 이론상 여기 도달 불가
                log.warn("예약 송금 실행 시 잔액 이상: scheduleId={}", schedule.getId());
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

            // 송금 실행 (차감은 이미 되어있으므로 입금만)
            toAccount.deposit(schedule.getAmount());
            transaction.complete();

            transactionRepository.save(transaction);
            accountRepository.save(toAccount);

            // 예약 완료 처리
            schedule.execute(transaction);
            scheduledTransferRepository.save(schedule);

            log.info("예약 송금 실행 완료: scheduleId={}, transactionId={}",
                    schedule.getId(), transaction.getId());

            // 알림 생성
            sendScheduledTransferNotificationAsync(
                    fromAccount.getUser().getId(),
                    transaction.getId(),
                    schedule.getAmount(),
                    schedule.getDescription(),
                    true
            );

        } catch (Exception e) {
            // 실행 실패 시 잔액 환불
            try {
                Account fromAccount = accountRepository.findByIdForUpdate(schedule.getFromAccount().getId())
                        .orElse(null);
                if (fromAccount != null) {
                    fromAccount.deposit(schedule.getAmount());
                    accountRepository.save(fromAccount);
                    log.info("예약 송금 실패로 잔액 환불: scheduleId={}, amount={}",
                            schedule.getId(), schedule.getAmount());
                }
            } catch (Exception refundError) {
                log.error("잔액 환불 실패: scheduleId={}, error={}",
                        schedule.getId(), refundError.getMessage());
            }

            // 실행 실패 처리
            schedule.fail(e.getMessage());
            scheduledTransferRepository.save(schedule);

            log.error("예약 송금 실행 실패: scheduleId={}, error={}",
                    schedule.getId(), e.getMessage());

            // 실패 알림
            sendScheduledTransferNotificationAsync(
                    schedule.getFromAccount().getUser().getId(),
                    null,
                    schedule.getAmount(),
                    e.getMessage(),
                    false
            );
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
