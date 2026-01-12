package com.study.moneygo.simplepassword.controller;

import com.study.moneygo.simplepassword.dto.request.SimplePasswordChangeRequest;
import com.study.moneygo.simplepassword.dto.request.SimplePasswordRegisterRequest;
import com.study.moneygo.simplepassword.dto.request.SimplePasswordVerifyRequest;
import com.study.moneygo.simplepassword.dto.response.SimplePasswordResponse;
import com.study.moneygo.simplepassword.service.SimplePasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/simple-password")
@RequiredArgsConstructor
public class SimplePasswordController {

    private final SimplePasswordService simplePasswordService;

    /**
     * 간편 비밀번호 등록
     *
     * @param request
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<SimplePasswordResponse> registerSimplePassword(
            @Valid @RequestBody SimplePasswordRegisterRequest request
    ) {
        log.info("간편 비밀번호 등록 요청");
        SimplePasswordResponse response = simplePasswordService.registerSimplePassword(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 간편 비밀번호 변경
     *
     * @param request
     * @return
     */
    @PatchMapping("/change")
    public ResponseEntity<SimplePasswordResponse> changeSimplePassword(
            @Valid @RequestBody SimplePasswordChangeRequest request
    ) {
        log.info("간편 비밀번호 변경 요청");
        SimplePasswordResponse response = simplePasswordService.changeSimplePassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 간편 비밀번호 확인
     * @param request
     * @return
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verifySimplePassword(
            @Valid @RequestBody SimplePasswordVerifyRequest request
    ) {
        log.info("간편 비밀번호 확인 요청");
        boolean isValid = simplePasswordService.verifySimplePassword(request);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @GetMapping("/status")
    public ResponseEntity<SimplePasswordResponse> checkSimplePasswordStatus() {
        log.info("간편 비밀번호 등록 여부 조회");
        SimplePasswordResponse response = simplePasswordService.checkSimplePasswordStatus();
        return ResponseEntity.ok(response);
    }
}
