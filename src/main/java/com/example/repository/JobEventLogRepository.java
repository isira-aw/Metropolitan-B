package com.example.repository;

import com.example.entity.JobEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobEventLogRepository extends JpaRepository<JobEventLog, Long> {

    List<JobEventLog> findByEventTimeBetween(LocalDateTime startDate, LocalDateTime endDate); // You can add custom queries here if necessary, e.g. finding logs by job ID
}