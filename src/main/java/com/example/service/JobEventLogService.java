package com.example.service;

import com.example.entity.JobEventLog;
import com.example.repository.JobEventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobEventLogService {

    @Autowired
    private JobEventLogRepository jobEventLogRepository;

    // Fetch logs by date range
    public List<JobEventLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return jobEventLogRepository.findByEventTimeBetween(startDate, endDate);
    }
}
