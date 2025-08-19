package com.example.met.service;

import com.example.met.dto.response.LogResponse;
import com.example.met.entity.Log;
import com.example.met.exception.ResourceNotFoundException;
import com.example.met.repository.LogRepository;
import com.example.met.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final LogRepository logRepository;

    @Transactional
    public Log createLog(Log logEntry) {
        // Ensure time and date are set if not provided
        if (logEntry.getTime() == null) {
            logEntry.setTime(TimeZoneUtil.getCurrentTime());
        }
        if (logEntry.getDate() == null) {
            logEntry.setDate(TimeZoneUtil.getCurrentDate());
        }

        Log savedLog = logRepository.save(logEntry);
        log.info("Log created with ID: {}", savedLog.getLogId());
        return savedLog;
    }

    public Log findById(UUID id) {
        return logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log not found with id: " + id));
    }

    public LogResponse getLogResponse(UUID id) {
        Log logEntry = findById(id);
        return convertToResponse(logEntry);
    }

    public List<LogResponse> getAllLogs() {
        log.info("Fetching all logs");
        return logRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<LogResponse> getLogsByEmployee(String email) {
        log.info("Fetching logs for employee: {}", email);
        return logRepository.findByEmployeeEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<LogResponse> getLogsByDate(LocalDate date) {
        log.info("Fetching logs for date: {}", date);
        return logRepository.findByDate(date)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<LogResponse> getLogsByEmployeeAndDate(String email, LocalDate date) {
        log.info("Fetching logs for employee: {} and date: {}", email, date);
        return logRepository.findByEmployeeEmailAndDate(email, date)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<LogResponse> getRecentLogs(int hours) {
        LocalDateTime fromDateTime = TimeZoneUtil.getCurrentDateTime().minusHours(hours);
        log.info("Fetching logs from last {} hours", hours);

        return logRepository.findRecentLogs(fromDateTime)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private LogResponse convertToResponse(Log logEntry) {
        LogResponse response = new LogResponse();
        response.setLogId(logEntry.getLogId());
        response.setEmployeeEmail(logEntry.getEmployee().getEmail());
        response.setEmployeeName(logEntry.getEmployee().getName());
        response.setAction(logEntry.getAction());
        response.setDate(logEntry.getDate());
        response.setTime(logEntry.getTime());
        response.setStatus(logEntry.getStatus());
        response.setLocation(logEntry.getLocation());
        response.setCreatedAt(logEntry.getCreatedAt());
        return response;
    }
}