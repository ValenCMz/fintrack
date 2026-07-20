package com.valencmz.fintrack.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valencmz.fintrack.model.entity.Monotributo;

public interface MonotributoRepository extends JpaRepository<Monotributo, UUID> {

    public List<Monotributo> findByUserId(UUID userId);

    public List<Monotributo> findByUserIdAndActive(UUID userId, boolean active);
}
