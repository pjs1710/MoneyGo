package com.study.moneygo.scheduled.transfer.repository;

import com.study.moneygo.scheduled.transfer.entity.ScheduledTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {

    // 사용자의 예약 송금 목록 조회
    @Query("SELECT st FROM ScheduledTransfer st WHERE st.fromAccount.user.id = :userId ORDER BY st.scheduledAt DESC")
    Page<ScheduledTransfer> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 실행 대기 중인 예약 조회
    @Query("SELECT st FROM ScheduledTransfer st WHERE st.status = 'PENDING' AND st.scheduledAt <= :now")
    List<ScheduledTransfer> findPendingSchedules(@Param("now") LocalDateTime now);

    // 특정 상태의 예약 조회
    @Query("SELECT st FROM ScheduledTransfer st WHERE st.fromAccount.user.id = :userId AND st.status = :status ORDER BY st.scheduledAt DESC")
    Page<ScheduledTransfer> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") ScheduledTransfer.ScheduleStatus status,
            Pageable pageable
    );
}
