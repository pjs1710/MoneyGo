package com.study.moneygo.favorite.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteRequest {

    @NotBlank(message = "계좌번호는 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{4}-\\d{4}$", message = "올바른 계좌번호 형식이 아닙니다.")
    private String accountNumber;

    @Size(max = 50, message = "별칭은 50자 이내로 입력해주세요.")
    private String nickname;

    @Size(max = 200, message = "메모는 200자 이내로 입력해주세요.")
    private String memo;
}
