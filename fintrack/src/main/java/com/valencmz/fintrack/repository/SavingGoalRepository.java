package com.valencmz.fintrack.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valencmz.fintrack.model.entity.SavingGoal;
import java.util.List;

public interface SavingGoalRepository extends JpaRepository<SavingGoal, UUID> {

    public List<SavingGoal> findByUserId(UUID userId);

    public List<SavingGoal> findByUserIdAndActive(UUID userId, boolean active);

}
