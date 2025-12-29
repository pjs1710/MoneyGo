package com.study.moneygo.account.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AccountOwnerResponse {

    private String accountNumber;
    private String ownerName;
    private String bankName;

    public static AccountOwnerResponse of(String accountNumber, String ownerName) {
        return AccountOwnerResponse.builder()
                .accountNumber(accountNumber)
                .ownerName(ownerName)
                .bankName("MoneyGO Bank")
                .build();
    }
}
