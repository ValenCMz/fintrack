package com.valencmz.fintrack.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valencmz.fintrack.model.entity.Card;

public interface CardRepository extends JpaRepository<Card, UUID> {

    public List<Card> findByUserId(UUID userId);

    public List<Card> findByUserIdAndActive(UUID userId, boolean active);

}
