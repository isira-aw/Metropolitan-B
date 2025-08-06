package com.example.repository;

import com.example.entity.JobEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobEventLogRepository extends JpaRepository<JobEventLog, Long> {

    // Get logs by date range
    List<JobEventLog> findByDateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Get daily logs
    @Query("SELECT j FROM JobEventLog j WHERE DATE(j.dateTime) = :date ORDER BY j.dateTime DESC")
    List<JobEventLog> findByDate(@Param("date") LocalDate date);

    // Get logs by email
    List<JobEventLog> findByEmailOrderByDateTimeDesc(String email);

    // Get logs by jobId
    List<JobEventLog> findByJobIdOrderByDateTimeDesc(String jobId);
}