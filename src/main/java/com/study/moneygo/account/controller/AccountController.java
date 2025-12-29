package com.study.moneygo.account.controller;


import com.study.moneygo.account.dto.response.AccountOwnerResponse;
import com.study.moneygo.account.dto.response.AccountResponse;
import com.study.moneygo.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // 내 계좌 조회
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount() {
        AccountResponse response = accountService.getMyAccount();
        return ResponseEntity.ok(response);
    }

    // 계좌 소유자 확인
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountOwnerResponse> getAccountOwner(@PathVariable String accountNumber) {
        AccountOwnerResponse response = accountService.getAccountOwner(accountNumber);
        return ResponseEntity.ok(response);
    }
}
