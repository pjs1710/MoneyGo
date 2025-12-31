package com.study.moneygo.favorite.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.util.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "favorites",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "account_number"})
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "account_owner_name", nullable = false, length = 50)
    private String accountOwnerName;

    @Column(length = 50)
    private String nickname;  // 사용자가 지정한 별칭 (예: "엄마", "회사 월급 계좌")

    @Column(length = 200)
    private String memo;  // 메모

    /** =====================================
     *              비즈니스 메서드
     *  ===================================== */

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
