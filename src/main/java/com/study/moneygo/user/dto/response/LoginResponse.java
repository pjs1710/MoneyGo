package com.study.moneygo.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long id;
        private String email;
        private String name;
        private String accountNumber;
    }

    public static LoginResponse of(
            String token, Long expiresIn, Long userId, String email, String name, String accountNumber
    ) {
        UserInfo userInfo = UserInfo.builder()
                .id(userId)
                .email(email)
                .name(name)
                .accountNumber(accountNumber)
                .build();

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userInfo)
                .build();
    }
}
