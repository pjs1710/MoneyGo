package com.study.moneygo.account.repository;

import com.study.moneygo.account.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // 특정 계좌의 모든 거래 내역
    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    // 특정 계좌의 송금 내역 (보낸 것)
    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findSentTransactions(@Param("accountId") Long accountId, Pageable pageable);

    // 특정 계좌의 수신 내역 (받은 것)
    @Query("SELECT t FROM Transaction t WHERE t.toAccount.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findReceivedTransactions(@Param("accountId") Long accountId, Pageable pageable);

    // 날짜 범위로 조회
    @Query("SELECT t FROM Transaction t WHERE (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId) " +
            "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}