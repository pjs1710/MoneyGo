package com.study.moneygo.simplepassword.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SimplePasswordChangeRequest {

    @NotBlank(message = "현재 간편 비밀번호는 필수입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "간편 비밀번호는 6자리 숫자여야 합니다.")
    private String currentSimplePassword;

    @NotBlank(message = "새 간편 비밀번호는 필수입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "간편 비밀번호는 6자리 숫자여야 합니다.")
    private String newSimplePassword;

    @NotBlank(message = "새 간편 비밀번호 확인은 필수입니다.")
    private String newSimplePasswordConfirm;
}
