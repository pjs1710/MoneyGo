package com.study.moneygo.favorite.dto.response;

import com.study.moneygo.favorite.entity.Favorite;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class FavoriteResponse {

    private Long favoriteId;
    private String accountNumber;
    private String accountOwnerName;
    private String nickname;
    private String memo;
    private LocalDateTime createdAt;

    public static FavoriteResponse of(Favorite favorite) {
        return FavoriteResponse.builder()
                .favoriteId(favorite.getId())
                .accountNumber(favorite.getAccountNumber())
                .accountOwnerName(favorite.getAccountOwnerName())
                .nickname(favorite.getNickname())
                .memo(favorite.getMemo())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
