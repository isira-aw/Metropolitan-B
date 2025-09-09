package com.example.met.service;

import com.example.met.dto.response.LogResponse;
import com.example.met.entity.Log;
import com.example.met.exception.ResourceNotFoundException;
import com.example.met.repository.LogRepository;
import com.example.met.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final LogRepository logRepository;

    // Sri Lanka timezone constant
    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");

    @Transactional
    public Log createLog(Log logEntry) {
        try {
            log.info("Creating log entry for action: {}", logEntry.getAction());

            // Validate log entry
            validateLogEntry(logEntry);

            // Ensure time and date are set if not provided with safe timezone handling
            if (logEntry.getTime() == null) {
                try {
                    logEntry.setTime(getSafeCurrentTime());
                } catch (DateTimeException e) {
                    log.warn("Error setting current time for log, using fallback: {}", e.getMessage());
                    logEntry.setTime(LocalTime.of(12, 0)); // Safe fallback
                }
            }

            if (logEntry.getDate() == null) {
                try {
                    logEntry.setDate(LocalDate.now(SRI_LANKA_ZONE));
                } catch (DateTimeException e) {
                    log.warn("Error setting current date for log, using fallback: {}", e.getMessage());
                    logEntry.setDate(LocalDate.now()); // System default fallback
                }
            }

            Log savedLog = logRepository.save(logEntry);
            log.info("Log created with ID: {}", savedLog.getLogId());
            return savedLog;
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating log entry", e);
            throw new IllegalArgumentException("Data integrity violation: invalid employee or other references", e);
        } catch (DataAccessException e) {
            log.error("Database error while creating log entry", e);
            throw new RuntimeException("Database error occurred while creating log entry", e);
        } catch (Exception e) {
            log.error("Unexpected error while creating log entry", e);
            throw new RuntimeException("Failed to create log entry: " + e.getMessage(), e);
        }
    }

    public Log findById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Log ID cannot be null");
            }

            return logRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Log not found with id: " + id));
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            // Re-throw these as they are already properly handled
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while finding log by ID: {}", id, e);
            throw new RuntimeException("Database error occurred while retrieving log", e);
        } catch (Exception e) {
            log.error("Unexpected error while finding log by ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve log", e);
        }
    }

    public LogResponse getLogResponse(UUID id) {
        try {
            Log logEntry = findById(id);
            return convertToResponse(logEntry);
        } catch (Exception e) {
            log.error("Error converting log to response for ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve log response", e);
        }
    }

    public List<LogResponse> getAllLogs() {
        try {
            log.info("Fetching all logs");
            return logRepository.findAll()
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Database error while fetching all logs", e);
            throw new RuntimeException("Database error occurred while retrieving logs", e);
        } catch (Exception e) {
            log.error("Error fetching all logs", e);
            throw new RuntimeException("Failed to retrieve logs", e);
        }
    }

    public List<LogResponse> getLogsByEmployee(String email) {
        try {
            log.info("Fetching logs for employee: {}", email);

            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Employee email cannot be null or empty");
            }

            // Basic email format validation
            if (!email.contains("@") || !email.contains(".")) {
                throw new IllegalArgumentException("Invalid email format");
            }

            return logRepository.findByEmployeeEmailOrderByCreatedAtDesc(email.trim())
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching logs for employee: {}", email, e);
            throw new RuntimeException("Database error occurred while retrieving logs for employee", e);
        } catch (Exception e) {
            log.error("Error fetching logs for employee: {}", email, e);
            throw new RuntimeException("Failed to retrieve logs for employee", e);
        }
    }

    public List<LogResponse> getLogsByDate(LocalDate date) {
        try {
            log.info("Fetching logs for date: {}", date);

            if (date == null) {
                throw new IllegalArgumentException("Date cannot be null");
            }

            // Optional: Validate date is not too far in the future
            if (date.isAfter(LocalDate.now().plusDays(1))) {
                throw new IllegalArgumentException("Date cannot be in the future");
            }

            return logRepository.findByDate(date)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching logs by date: {}", date, e);
            throw new RuntimeException("Database error occurred while retrieving logs by date", e);
        } catch (Exception e) {
            log.error("Error fetching logs by date: {}", date, e);
            throw new RuntimeException("Failed to retrieve logs by date", e);
        }
    }

    public List<LogResponse> getLogsByEmployeeAndDate(String email, LocalDate date) {
        try {
            log.info("Fetching logs for employee: {} and date: {}", email, date);

            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Employee email cannot be null or empty");
            }

            if (date == null) {
                throw new IllegalArgumentException("Date cannot be null");
            }

            // Basic email format validation
            if (!email.contains("@") || !email.contains(".")) {
                throw new IllegalArgumentException("Invalid email format");
            }

            // Date validation
            if (date.isAfter(LocalDate.now().plusDays(1))) {
                throw new IllegalArgumentException("Date cannot be in the future");
            }

            return logRepository.findByEmployeeEmailAndDate(email.trim(), date)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching logs for employee: {} and date: {}", email, date, e);
            throw new RuntimeException("Database error occurred while retrieving logs", e);
        } catch (Exception e) {
            log.error("Error fetching logs for employee: {} and date: {}", email, date, e);
            throw new RuntimeException("Failed to retrieve logs for employee and date", e);
        }
    }

    public List<LogResponse> getRecentLogs(int hours) {
        try {
            log.info("Fetching logs from last {} hours", hours);

            if (hours <= 0) {
                throw new IllegalArgumentException("Hours must be positive");
            }

            if (hours > 8760) { // More than a year
                throw new IllegalArgumentException("Hours cannot exceed 8760 (1 year)");
            }

            LocalDateTime fromDateTime;
            try {
                fromDateTime = getSafeCurrentDateTime().minusHours(hours);
            } catch (DateTimeException e) {
                log.warn("Error calculating datetime for recent logs, using system default: {}", e.getMessage());
                fromDateTime = LocalDateTime.now().minusHours(hours).withNano(0);
            }

            return logRepository.findRecentLogs(fromDateTime)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching recent logs for {} hours", hours, e);
            throw new RuntimeException("Database error occurred while retrieving recent logs", e);
        } catch (Exception e) {
            log.error("Error fetching recent logs for {} hours", hours, e);
            throw new RuntimeException("Failed to retrieve recent logs", e);
        }
    }

    private LogResponse convertToResponse(Log logEntry) {
        try {
            if (logEntry == null) {
                throw new IllegalArgumentException("Log entry cannot be null");
            }

            LogResponse response = new LogResponse();
            response.setLogId(logEntry.getLogId());
            response.setGeneratorName(logEntry.getGeneratorName());
            // Safe employee data extraction
            if (logEntry.getEmployee() != null) {
                response.setEmployeeEmail(logEntry.getEmployee().getEmail());
                response.setEmployeeName(logEntry.getEmployee().getName());
            } else {
                log.warn("Log entry {} has null employee reference", logEntry.getLogId());
                response.setEmployeeEmail("UNKNOWN");
                response.setEmployeeName("UNKNOWN");
            }

            response.setAction(logEntry.getAction());
            response.setDate(logEntry.getDate());
            response.setTime(logEntry.getTime());
            response.setStatus(logEntry.getStatus());
            response.setLocation(logEntry.getLocation());
            response.setCreatedAt(logEntry.getCreatedAt());

            return response;
        } catch (Exception e) {
            log.error("Error converting log entry to response", e);
            throw new RuntimeException("Failed to convert log entry to response", e);
        }
    }

    // Utility methods for safe time handling
    private LocalTime getSafeCurrentTime() {
        try {
            return LocalTime.now(SRI_LANKA_ZONE).withNano(0); // Remove nanoseconds to prevent precision issues
        } catch (DateTimeException e) {
            log.warn("Error getting current time with timezone, using system default: {}", e.getMessage());
            return LocalTime.now().withNano(0); // System default without nanoseconds
        }
    }

    private LocalDateTime getSafeCurrentDateTime() {
        try {
            return LocalDateTime.now(SRI_LANKA_ZONE).withNano(0);
        } catch (DateTimeException e) {
            log.warn("Error getting current datetime with timezone, using system default: {}", e.getMessage());
            return LocalDateTime.now().withNano(0); // System default without nanoseconds
        }
    }

    // Validation methods
    private void validateLogEntry(Log logEntry) {
        if (logEntry == null) {
            throw new IllegalArgumentException("Log entry cannot be null");
        }
        if (logEntry.getEmployee() == null) {
            throw new IllegalArgumentException("Employee reference cannot be null");
        }
        if (logEntry.getAction() == null || logEntry.getAction().trim().isEmpty()) {
            throw new IllegalArgumentException("Action cannot be null or empty");
        }
        if (logEntry.getAction().length() > 255) { // Assuming max length
            throw new IllegalArgumentException("Action description cannot exceed 255 characters");
        }
        if (logEntry.getStatus() != null && logEntry.getStatus().length() > 500) { // Assuming max length
            throw new IllegalArgumentException("Status description cannot exceed 500 characters");
        }
        if (logEntry.getLocation() != null && logEntry.getLocation().length() > 255) { // Assuming max length
            throw new IllegalArgumentException("Location cannot exceed 255 characters");
        }
    }
}