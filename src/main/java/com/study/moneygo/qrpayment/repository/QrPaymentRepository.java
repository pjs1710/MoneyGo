package com.study.moneygo.qrpayment.repository;

import com.study.moneygo.qrpayment.entity.QrPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrPaymentRepository extends JpaRepository<QrPayment, Long> {
    Optional<QrPayment> findByQrCode(String qrCode);
    boolean existsByQrCode(String qrCode);
}
