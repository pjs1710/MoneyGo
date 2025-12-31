package com.study.moneygo.scheduled.transfer.executor;

import com.study.moneygo.scheduled.transfer.entity.ScheduledTransfer;
import com.study.moneygo.scheduled.transfer.repository.ScheduledTransferRepository;
import com.study.moneygo.scheduled.transfer.service.ScheduledTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTransferExecutor {

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final ScheduledTransferService scheduledTransferService;

    // 매분 실행 - 실행 대기 중인 예약 송금 확인
    @Scheduled(cron = "0 * * * * *")  // 매분 0초에 실행
    public void executeScheduledTransfers() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("예약 송금 스케줄러 실행: {}", now);

        try {
            // 실행 대기 중인 예약 조회
            List<ScheduledTransfer> pendingSchedules = scheduledTransferRepository.findPendingSchedules(now);

            if (!pendingSchedules.isEmpty()) {
                log.info("실행 대기 중인 예약 송금 {}건 발견", pendingSchedules.size());

                for (ScheduledTransfer schedule : pendingSchedules) {
                    try {
                        log.info("예약 송금 실행 시작: scheduleId={}, scheduledAt={}",
                                schedule.getId(), schedule.getScheduledAt());

                        scheduledTransferService.executeScheduledTransfer(schedule);

                    } catch (Exception e) {
                        log.error("예약 송금 실행 중 오류 발생: scheduleId={}, error={}",
                                schedule.getId(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("예약 송금 스케줄러 실행 중 오류 발생", e);
        }
    }
}
