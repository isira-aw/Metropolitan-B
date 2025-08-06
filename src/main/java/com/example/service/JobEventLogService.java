// 3. JobEventLogService
package com.example.service;

import com.example.entity.JobEventLog;
import com.example.repository.JobEventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobEventLogService {

    @Autowired
    private JobEventLogRepository jobEventLogRepository;

    public JobEventLog createLog(String name, String email, String generatorId,
                                 String location, String jobId, String action) {
        JobEventLog log = new JobEventLog(name, email, generatorId,
                LocalDateTime.now(), location, jobId, action);
        return jobEventLogRepository.save(log);
    }

    public List<JobEventLog> getDailyLogs(LocalDate date) {
        return jobEventLogRepository.findByDate(date);
    }

    public List<JobEventLog> getTodayLogs() {
        return getDailyLogs(LocalDate.now());
    }

    public List<JobEventLog> getLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return jobEventLogRepository.findByDateTimeBetween(startDate, endDate);
    }

    public List<JobEventLog> getLogsByEmail(String email) {
        return jobEventLogRepository.findByEmailOrderByDateTimeDesc(email);
    }

    public List<JobEventLog> getLogsByJobId(String jobId) {
        return jobEventLogRepository.findByJobIdOrderByDateTimeDesc(jobId);
    }
}
