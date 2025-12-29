package com.study.moneygo.account.repository;

import com.study.moneygo.account.entity.TransferLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferLimitRepository extends JpaRepository<TransferLimit, Long> {

    Optional<TransferLimit> findByAccountId(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tl FROM TransferLimit tl WHERE tl.account.id = :accountId")
    Optional<TransferLimit> findByAccountIdForUpdate(@Param("accountId") Long accountId);
}
