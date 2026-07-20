package com.valencmz.fintrack.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.valencmz.fintrack.model.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    public User findByUsername(String username);

    public User findByEmail(String email);

}
