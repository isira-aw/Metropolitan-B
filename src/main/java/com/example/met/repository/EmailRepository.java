package com.example.met.repository;


import com.example.met.entity.EmailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailRepository extends JpaRepository<EmailEntity, UUID> {
    List<EmailEntity> findBySentByOrderByCreatedAtDesc(String sentBy);
}
