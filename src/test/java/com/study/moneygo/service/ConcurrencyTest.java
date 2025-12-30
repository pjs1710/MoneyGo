package com.study.moneygo.service;

import com.study.moneygo.account.dto.request.TransferRequest;
import com.study.moneygo.account.dto.response.TransferResponse;
import com.study.moneygo.account.entity.Account;
import com.study.moneygo.account.repository.AccountRepository;
import com.study.moneygo.account.repository.TransactionRepository;
import com.study.moneygo.account.repository.TransferLimitRepository;
import com.study.moneygo.account.service.TransferService;
import com.study.moneygo.qrpayment.repository.QrPaymentRepository;
import com.study.moneygo.user.entity.User;
import com.study.moneygo.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class ConcurrencyTest {
    @Autowired
    private TransferService transferService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QrPaymentRepository qrPaymentRepository;

    @Autowired
    private TransferLimitRepository transferLimitRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    private User sender;
    private User receiver;
    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    @Transactional
    public void setUp() {
        qrPaymentRepository.deleteAll();
        transactionRepository.deleteAll();
        transferLimitRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();;

        // 송금자 생성
        sender = User.builder()
                .email("sender@test.com")
                .password(passwordEncoder.encode("Test1234!"))
                .name("송금자")
                .phone("010-1111-1111")
                .status(User.UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build();
        sender = userRepository.save(sender);

        // 수신자 생성
        receiver = User.builder()
                .email("receiver@test.com")
                .password(passwordEncoder.encode("Test1234!"))
                .name("수신자")
                .phone("010-2222-2222")
                .status(User.UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build();
        receiver = userRepository.save(receiver);

        // 송금자 계좌 생성 (초기 잔액은 100만원으로)
        senderAccount = Account.builder()
                .user(sender)
                .accountNumber("1001-0001-0001")
                .balance(new BigDecimal("1000000.00"))
                .status(Account.AccountStatus.ACTIVE)
                .build();
        senderAccount = accountRepository.save(senderAccount);

        // 수신자 계좌 생성
        receiverAccount = Account.builder()
                .user(receiver)
                .accountNumber("1001-0002-0002")
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .build();
        receiverAccount = accountRepository.save(receiverAccount);
    }

    @Test
    @DisplayName("동시에 10명이 같은 계좌에서 10만원씩 송금 - 잔액 음수 방지 테스트")
    public void concurrentTransferTest() throws InterruptedException {
        // given
        // 10개의 동시 송금 요청
        int threadCnt = 10;
        BigDecimal transferAmount = new BigDecimal("100000");
        BigDecimal initialBalance = senderAccount.getBalance(); // 100만원

        ExecutorService executorService = Executors.newFixedThreadPool(threadCnt);
        CountDownLatch latch = new CountDownLatch(threadCnt);

        AtomicInteger successCnt = new AtomicInteger(0);
        AtomicInteger failCnt = new AtomicInteger(0);

        // when
        // 동시에 10번 송금 시도
        for (int i = 0; i < threadCnt; i++) {
            executorService.submit(() -> {
                try {
                    setAuthentication("sender@test.com");

                    TransferRequest request = new TransferRequest(
                            receiverAccount.getAccountNumber(),
                            transferAmount,
                            "동시성 테스트",
                            "Test1234!"
                    );

                    TransferResponse response = transferService.transfer(request);
                    successCnt.incrementAndGet();
                    System.out.println("=== 송금 성공 : " + Thread.currentThread().getName() + " ===");
                } catch (Exception e) {
                    failCnt.incrementAndGet();
                    System.out.println("=== 송금 실패 : " + e.getMessage() + " ===");
                } finally {
                    SecurityContextHolder.clearContext();
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드 완료 대기
        executorService.shutdown();

        // then
        Account updatedSenderAccount = accountRepository.findById(senderAccount.getId()).orElseThrow();
        Account updatedReceiverAccount = accountRepository.findById(receiverAccount.getId()).orElseThrow();

        System.out.println("\n===== 테스트 결과 =====");
        System.out.println("성공 횟수 : " + successCnt.get());
        System.out.println("실패 횟수 : " + failCnt.get());
        System.out.println("송금자 최종 잔액 : " + updatedSenderAccount.getBalance());
        System.out.println("수신자 최종 잔액 : " + updatedReceiverAccount.getBalance());
        System.out.println("=======================\n");

        BigDecimal expectedSenderBalance = initialBalance.subtract(
                transferAmount.multiply(new BigDecimal(successCnt.get()))
        );
        BigDecimal expectedReceiverBalance = transferAmount.multiply(
                new BigDecimal(successCnt.get())
        );

        assertThat(updatedSenderAccount.getBalance()).isEqualTo(expectedSenderBalance);
        assertThat(updatedReceiverAccount.getBalance()).isEqualTo(expectedReceiverBalance);
        assertThat(updatedSenderAccount.getBalance().compareTo(BigDecimal.ZERO)).isGreaterThanOrEqualTo(0);
        assertThat(successCnt.get()).isEqualTo(10);
    }

    private void setAuthentication(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
