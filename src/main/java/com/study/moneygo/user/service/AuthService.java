package com.study.moneygo.user.service;

import com.study.moneygo.account.entity.Account;
import com.study.moneygo.account.repository.AccountRepository;
import com.study.moneygo.user.dto.request.LoginRequest;
import com.study.moneygo.user.dto.request.SignupRequest;
import com.study.moneygo.user.dto.response.LoginResponse;
import com.study.moneygo.user.dto.response.SignupResponse;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import com.study.moneygo.util.account.AccountNumberGenerator;
import com.study.moneygo.util.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 이메일 중복 유효성 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .status(User.UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build();

        User savedUser = userRepository.save(user);

        // 계좌 생성하기
        String accountNumber = generateUniqueAccountNumber();
        Account account = Account.builder()
                .user(savedUser)
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        Account savedAccount = accountRepository.save(account);

        return SignupResponse.of(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedAccount.getAccountNumber(),
                savedAccount.getBalance()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        // 계정 Lock 체크
        if (user.getStatus() == User.UserStatus.LOCKED) {
            throw new IllegalArgumentException("계정이 잠겨있습니다. 관리자에게 문의하세요.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            // 로그인 성공하면 failedAttempts 초기화 해주기
            user.resetFailedAttempts();
            userRepository.save(user);

            String token = jwtTokenProvider.generateToken(authentication);
            Long expiresIn = jwtTokenProvider.getExpirationTime();

            // 계좌 정보 조회하기
            Account account = accountRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("계좌 정보를 찾을 수 없습니다."));

            return LoginResponse.of(
                    token,
                    expiresIn,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    account.getAccountNumber()
            );
        } catch (Exception e) {
            // 로그인 로직에 실패했으니 failedAttempts 카운트 증가
            user.incrementFailedAttempts();
            userRepository.save(user);
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = AccountNumberGenerator.generate();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}
