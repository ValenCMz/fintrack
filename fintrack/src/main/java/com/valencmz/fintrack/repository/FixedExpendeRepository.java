package com.valencmz.fintrack.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valencmz.fintrack.model.entity.FixedExpende;

public interface FixedExpendeRepository extends JpaRepository<FixedExpende, UUID> {

    public List<FixedExpende> findByUserId(UUID userId);

    public List<FixedExpende> findByUserIdAndActive(UUID userId, boolean active);

}
