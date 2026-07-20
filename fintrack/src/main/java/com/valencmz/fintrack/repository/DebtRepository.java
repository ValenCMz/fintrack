package com.valencmz.fintrack.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valencmz.fintrack.enums.DebtStatus;
import com.valencmz.fintrack.model.entity.Debt;

public interface DebtRepository extends JpaRepository<Debt, UUID> {

    public List<Debt> findByUserId(UUID userId);

    public List<Debt> findByUserIdAndStatus(UUID userId, DebtStatus status);

}
