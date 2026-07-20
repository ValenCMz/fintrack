package com.valencmz.fintrack.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valencmz.fintrack.enums.TransactionType;
import com.valencmz.fintrack.model.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    public List<Transaction> findByUserId(UUID userId);

    public List<Transaction> findByUserIdAndType(UUID userId, TransactionType type);

    public List<Transaction> findByUserIdAndDateBetween(UUID userId, LocalDate from, LocalDate to);

}
