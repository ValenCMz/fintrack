package com.valencmz.fintrack.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.valencmz.fintrack.model.entity.CardPayment;

public interface CardPaymentRepository extends JpaRepository<CardPayment, UUID> {

    public List<CardPayment> findByUserId(UUID userId);

    public List<CardPayment> findByCardIdAndUserId(UUID cardId, UUID userId);

    @Modifying
    @Query("delete from CardPayment p where p.cardInstallment.transaction.id = :transactionId")
    void deleteByInstallmentTransactionId(@Param("transactionId") UUID transactionId);
}
