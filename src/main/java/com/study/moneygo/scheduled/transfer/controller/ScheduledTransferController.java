package com.study.moneygo.scheduled.transfer.controller;

import com.study.moneygo.scheduled.transfer.dto.request.ScheduledTransferRequest;
import com.study.moneygo.scheduled.transfer.dto.response.ScheduledTransferResponse;
import com.study.moneygo.scheduled.transfer.service.ScheduledTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduled-transfers")
@RequiredArgsConstructor
public class ScheduledTransferController {

    private final ScheduledTransferService scheduledTransferService;

    /*
     송금 예약 생성
     */
    @PostMapping
    public ResponseEntity<ScheduledTransferResponse> createSchedule(
            @Valid @RequestBody ScheduledTransferRequest request) {
        ScheduledTransferResponse response = scheduledTransferService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     내 송금 예약 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<ScheduledTransferResponse>> getMySchedules(
            @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<ScheduledTransferResponse> schedules = scheduledTransferService.getMySchedules(pageable);
        return ResponseEntity.ok(schedules);
    }

    /*
     송금 예약 상세 조회
     */
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduledTransferResponse> getScheduleDetail(@PathVariable Long scheduleId) {
        ScheduledTransferResponse response = scheduledTransferService.getScheduleDetail(scheduleId);
        return ResponseEntity.ok(response);
    }

    /*
     송금 예약 취소
     */
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> cancelSchedule(@PathVariable Long scheduleId) {
        scheduledTransferService.cancelSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }
}
