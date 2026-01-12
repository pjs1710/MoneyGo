package com.study.moneygo.simplepassword.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SimplePasswordResponse {

    private boolean hasSimplePassword;
    private String message;

    public static SimplePasswordResponse of(boolean hasSimplePassword, String message) {
        return SimplePasswordResponse.builder()
                .hasSimplePassword(hasSimplePassword)
                .message(message)
                .build();
    }
}
