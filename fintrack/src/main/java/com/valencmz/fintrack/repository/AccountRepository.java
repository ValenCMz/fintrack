package com.valencmz.fintrack.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valencmz.fintrack.model.entity.Account;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    public List<Account> findByUserId(UUID userId);

    public List<Account> findByUserIdAndActive(UUID userId, boolean active);

}
