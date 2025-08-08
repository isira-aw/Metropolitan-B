package com.example.repository;

import com.example.entity.JobEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

public interface JobEventLogRepository extends JpaRepository<JobEventLog, Long> {

    // Corrected the query method to use the eventTime field
    List<JobEventLog> findByEventTimeBetween(LocalDateTime start, LocalDateTime end);
}