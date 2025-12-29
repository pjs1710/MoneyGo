package com.study.moneygo.admin.controller;

import com.study.moneygo.admin.dto.request.AdminDepositRequest;
import com.study.moneygo.admin.dto.request.AdminWithdrawRequest;
import com.study.moneygo.admin.dto.response.AdminDepositResponse;
import com.study.moneygo.admin.dto.response.AdminWithdrawResponse;
import com.study.moneygo.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<AdminDepositResponse> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody AdminDepositRequest request
    ) {
        AdminDepositResponse response = adminService.deposit(accountId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<AdminWithdrawResponse> withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody AdminWithdrawRequest request
    ) {
        AdminWithdrawResponse response = adminService.withdraw(accountId, request);
        return ResponseEntity.ok(response);
    }
}
