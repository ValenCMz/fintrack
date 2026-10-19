package com.valencmz.fintrack.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.valencmz.fintrack.model.entity.CardInstallment;

public interface CardInstallmentRepository extends JpaRepository<CardInstallment, UUID> {

    public List<CardInstallment> findByCardIdAndPaidOrderByDueDateAsc(UUID cardId, boolean paid);

    public List<CardInstallment> findByCardIdOrderByDueDateAsc(UUID cardId);

    @Query("select i from CardInstallment i where i.transaction.id = :transactionId and i.paid = false order by i.number")
    public List<CardInstallment> findUnpaidByTransactionId(@Param("transactionId") UUID transactionId);

    public Optional<CardInstallment> findByIdAndCardIdAndCardUserId(UUID id, UUID cardId, UUID userId);
}
