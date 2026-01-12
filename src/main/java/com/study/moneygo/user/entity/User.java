package com.study.moneygo.user.entity;

import com.study.moneygo.util.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Setter
    @Column(name = "simple_password", length = 100)
    private String simplePassword; // 간편 비밀번호 6자리, 암호화 저장

    @Column(name = "failed_simple_password_attempts")
    private Integer failedSimplePasswordAttempts = 0;

    /** =====================================
     *              비즈니스 메서드
     *  ===================================== */

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        // 로그인 시도 3회 이상 실패 시 LOCK
        if (this.failedLoginAttempts >= 3) {
            this.status = UserStatus.LOCKED;
        }
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
    }

    public void incrementFailedSimplePasswordAttempts() {
        this.failedSimplePasswordAttempts++;
        // 간편 비밀번호 5회 이상 실패 시 LOCK
        if (this.failedSimplePasswordAttempts >= 5) {
            this.status = UserStatus.LOCKED;
        }
    }

    public void resetFailedSimplePasswordAttempts() {
        this.failedSimplePasswordAttempts = 0;
    }

    public boolean hasSimplePassword() {
        return this.simplePassword != null && !this.simplePassword.isEmpty();
    }

    public void lock() {
        this.status = UserStatus.LOCKED;
    }

    public void unlock() {
        this.status = UserStatus.ACTIVE;
        this.failedLoginAttempts = 0;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public enum UserStatus {
        ACTIVE,
        LOCKED,
        SUSPENDED
    }

}
