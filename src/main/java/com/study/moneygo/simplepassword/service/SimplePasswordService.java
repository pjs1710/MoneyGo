package com.study.moneygo.simplepassword.service;

import com.study.moneygo.simplepassword.dto.request.SimplePasswordChangeRequest;
import com.study.moneygo.simplepassword.dto.request.SimplePasswordRegisterRequest;
import com.study.moneygo.simplepassword.dto.request.SimplePasswordVerifyRequest;
import com.study.moneygo.simplepassword.dto.response.SimplePasswordResponse;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimplePasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 간편 비밀번호 등록
     * @param request
     * @return
     */
    @Transactional
    public SimplePasswordResponse registerSimplePassword(SimplePasswordRegisterRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이미 간편 비밀번호가 등록되어 있는지 확인
        if (user.hasSimplePassword()) {
            throw new IllegalStateException("이미 간편 비밀번호가 등록되어 있습니다.");
        }

        // 로그인 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("로그인 비밀번호가 올바르지 않습니다.");
        }

        // 간편 비밀번호 확인
        if (!request.getSimplePassword().equals(request.getSimplePasswordConfirm())) {
            throw new IllegalArgumentException("간편 비밀번호가 일치하지 않습니다.");
        }

        // 다 통과하면 간편 비밀번호 등록 가능 -> 암호화 저장
        String encodedSimplePassword = passwordEncoder.encode(request.getSimplePassword());
        user.setSimplePassword(encodedSimplePassword);
        userRepository.save(user);

        log.info("간편 비밀번호 등록 완료: userId={}", user.getId());
        return SimplePasswordResponse.of(true, "간편 비밀번호가 등록되었습니다.");
    }

    /**
     * 간편 비밀번호 변경
     * @param request
     * @return
     */
    @Transactional
    public SimplePasswordResponse changeSimplePassword(SimplePasswordChangeRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 간편 비밀번호가 등록되어 있는지 확인(등록되어 있어야 변경 가능함)
        if (!user.hasSimplePassword()) {
            throw new IllegalStateException("등록된 간편 비밀번호가 없습니다.");
        }

        // 현재 간편 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentSimplePassword(), user.getSimplePassword())) {
            user.incrementFailedSimplePasswordAttempts();
            userRepository.save(user);

            int remainingAttempts = 5 - user.getFailedSimplePasswordAttempts();
            if (remainingAttempts <= 0) {
                throw new IllegalStateException("간편 비밀번호 입력 실패 횟수 초과로 계정이 잠겼습니다.");
            }

            throw new IllegalArgumentException(
                    String.format("현재 간편 비밀번호가 올바르지 않습니다. (남은 시도 : %d회)", remainingAttempts)
            );
        }

        // 새 비밀번호와 확인
        if (!request.getNewSimplePassword().equals(request.getNewSimplePasswordConfirm())) {
            throw new IllegalArgumentException("새 간편 비밀번호가 일치하지 않습니다.");
        }

        // 현재 비밀번호와 새 비밀번호가 같은지 확인
        if (request.getCurrentSimplePassword().equals(request.getNewSimplePassword())) {
            throw new IllegalArgumentException("새 간편 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        // 다 통과 시 비밀번호 변경
        String encodedSimplePassword = passwordEncoder.encode(request.getNewSimplePassword());
        user.setSimplePassword(encodedSimplePassword);
        user.resetFailedSimplePasswordAttempts();
        userRepository.save(user);

        log.info("간편 비밀번호 변경 완료: userId={}", user.getId());
        return SimplePasswordResponse.of(true, "간편 비밀번호가 변경되었습니다.");
    }

    /**
     * 간편 비밀번호 확인 (검증)
     * @param request
     * @return
     */
    @Transactional
    public boolean verifySimplePassword(SimplePasswordVerifyRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 간편 비밀번호가 등록되어 있는지 확인
        if (!user.hasSimplePassword()) {
            throw new IllegalStateException("등록된 간편 비밀번호가 없습니다.");
        }

        // 간편 비밀번호 확인
        if (!passwordEncoder.matches(request.getSimplePassword(), user.getSimplePassword())) {
            user.incrementFailedSimplePasswordAttempts();
            userRepository.save(user);

            int remainingAttempts = 5 - user.getFailedSimplePasswordAttempts();
            if (remainingAttempts <= 0) {
                throw new IllegalStateException("간편 비밀번호 입력 실패 횟수 초과로 계정이 잠겼습니다.");
            }

            throw new IllegalArgumentException(
                    String.format("간편 비밀번호가 올바르지 않습니다. (남은 시도: %d회)", remainingAttempts)
            );
        }

        // 다 통과 시 실패 횟수 초기화
        user.resetFailedSimplePasswordAttempts();
        userRepository.save(user);

        log.info("간편 비밀번호 확인 성공: userId={}", user.getId());
        return true;
    }

    /**
     * 간편 비밀번호 등록 여부 확인
     */
    @Transactional(readOnly = true)
    public SimplePasswordResponse checkSimplePasswordStatus() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        boolean hasSimplePassword = user.hasSimplePassword();
        String message = hasSimplePassword ? "간편 비밀번호가 등록되어 있습니다." : "간편 비밀번호가 등록되어 있지 않습니다.";

        return SimplePasswordResponse.of(hasSimplePassword, message);
    }

    /**
     * 간편 비밀번호 검증 (내부 사용)
     * @param userId 사용자 ID
     * @param simplePassword 검증할 간편 비밀번호
     * @return 검증 성공 여부
     */
    @Transactional
    public boolean verifySimplePasswordForUser(Long userId, String simplePassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 간편 비밀번호가 등록되어 있는지 확인
        if (!user.hasSimplePassword()) {
            throw new IllegalStateException("등록된 간편 비밀번호가 없습니다. 먼저 간편 비밀번호를 등록해주세요.");
        }

        // 간편 비밀번호 확인
        if (!passwordEncoder.matches(simplePassword, user.getSimplePassword())) {
            user.incrementFailedSimplePasswordAttempts();
            userRepository.save(user);

            int remainingAttempts = 5 - user.getFailedSimplePasswordAttempts();
            if (remainingAttempts <= 0) {
                throw new IllegalStateException("간편 비밀번호 입력 실패 횟수 초과로 계정이 잠겼습니다.");
            }
            throw new IllegalArgumentException(
                    String.format("간편 비밀번호가 올바르지 않습니다. (남은 시도: %d회)", remainingAttempts)
            );
        }

        // 성공 시 실패 횟수 초기화
        user.resetFailedSimplePasswordAttempts();
        userRepository.save(user);

        return true;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

}
