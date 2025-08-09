package com.example.met.service;

import com.example.met.dto.response.LogResponse;
import com.example.met.entity.Log;
import com.example.met.exception.ResourceNotFoundException;
import com.example.met.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final LogRepository logRepository;

    @Transactional
    public Log createLog(Log log) {
        Log savedLog = logRepository.save(log);
//        log.info("Log created with ID: {}", savedLog.getLogId());
        return savedLog;
    }

    public Log findById(UUID id) {
        return logRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log not found with id: " + id));
    }

    public LogResponse getLogResponse(UUID id) {
        Log log = findById(id);
        return convertToResponse(log);
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
        LocalDateTime fromDateTime = LocalDateTime.now().minusHours(hours);
        log.info("Fetching logs from last {} hours", hours);
        return logRepository.findRecentLogs(fromDateTime)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private LogResponse convertToResponse(Log log) {
        LogResponse response = new LogResponse();
        response.setLogId(log.getLogId());
        response.setEmployeeEmail(log.getEmployee().getEmail());
        response.setEmployeeName(log.getEmployee().getName());
        response.setAction(log.getAction());
        response.setDate(log.getDate());
        response.setTime(log.getTime());
        response.setStatus(log.getStatus());
        response.setLocation(log.getLocation());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}