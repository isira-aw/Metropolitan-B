package com.example.met.repository;

import com.example.met.entity.EmailRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailRecordRepository extends JpaRepository<EmailRecord, UUID> {

    List<EmailRecord> findByJobCardIdOrderByCreatedAtDesc(UUID jobCardId);

    List<EmailRecord> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    List<EmailRecord> findByStatusOrderByCreatedAtDesc(EmailRecord.EmailStatus status);

    long countByStatus(EmailRecord.EmailStatus status);
}