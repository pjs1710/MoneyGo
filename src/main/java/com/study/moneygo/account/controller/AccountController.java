package com.study.moneygo.account.controller;


import com.study.moneygo.account.dto.response.AccountOwnerResponse;
import com.study.moneygo.account.dto.response.AccountResponse;
import com.study.moneygo.account.service.AccountService;
import com.study.moneygo.deposit.dto.request.SelfDepositRequest;
import com.study.moneygo.deposit.dto.response.SelfDepositResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /*
     내 계좌 조회
     */
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount() {
        AccountResponse response = accountService.getMyAccount();
        return ResponseEntity.ok(response);
    }

    /*
     계좌 소유자 확인
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountOwnerResponse> getAccountOwner(@PathVariable String accountNumber) {
        AccountOwnerResponse response = accountService.getAccountOwner(accountNumber);
        return ResponseEntity.ok(response);
    }

    /*
     본인 계좌 입금
     */
    @PostMapping("/deposit")
    public ResponseEntity<SelfDepositResponse> selfDeposit(
            @Valid @RequestBody SelfDepositRequest request
    ) {
        log.info("본인 계좌 입금 요청: amount={}", request.getAmount());
        SelfDepositResponse response = accountService.selfDeposit(request);
        return ResponseEntity.ok(response);
    }
}
