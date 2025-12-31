package com.study.moneygo.favorite.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteUpdateRequest {

    @Size(max = 50, message = "별칭은 50자 이내로 입력해주세요.")
    private String nickname;

    @Size(max = 200, message = "메모는 200자 이내로 입력해주세요.")
    private String memo;
}
