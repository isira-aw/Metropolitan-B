package com.example.met.controller;

import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.LogResponse;
import com.example.met.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {

    private final LogService logService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getAllLogs() {
        try {
            log.info("Request to get all logs");

            List<LogResponse> logs = logService.getAllLogs();
            ApiResponse<List<LogResponse>> response = ApiResponse.success(
                    "Logs retrieved successfully", logs);

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.error("Security error while retrieving all logs", e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error(
                    "Access denied. Admin privileges required", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("Error retrieving all logs", e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error(
                    "Failed to retrieve logs", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LogResponse>> getLogById(@PathVariable UUID id) {
        try {
            log.info("Request to get log by ID: {}", id);

            LogResponse logResponse = logService.getLogResponse(id);
            ApiResponse<LogResponse> response = ApiResponse.success(
                    "Log retrieved successfully", logResponse);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Log not found with ID: {}", id, e);
            ApiResponse<LogResponse> response = ApiResponse.error("Log not found with the provided ID", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving log by ID: {}", id, e);
            ApiResponse<LogResponse> response = ApiResponse.error(
                    "Failed to retrieve log", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/employee/{email}")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getLogsByEmployee(@PathVariable String email) {
        try {
            log.info("Request to get logs for employee: {}", email);

            // Validate email parameter
            if (email == null || email.trim().isEmpty()) {
                ApiResponse<List<LogResponse>> response = ApiResponse.error("Employee email cannot be empty", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Basic email format validation
            if (!email.contains("@") || !email.contains(".")) {
                ApiResponse<List<LogResponse>> response = ApiResponse.error("Invalid email format", null);
                return ResponseEntity.badRequest().body(response);
            }

            List<LogResponse> logs = logService.getLogsByEmployee(email);
            ApiResponse<List<LogResponse>> response = ApiResponse.success(
                    "Logs retrieved successfully", logs);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid employee email: {}", email, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error("Invalid employee email: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error retrieving logs for employee: {}", email, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error(
                    "Failed to retrieve logs for employee", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getLogsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            log.info("Request to get logs for date: {}", date);

            // Validate date is not in the future
            if (date.isAfter(LocalDate.now())) {
                ApiResponse<List<LogResponse>> response = ApiResponse.error("Date cannot be in the future", null);
                return ResponseEntity.badRequest().body(response);
            }

            List<LogResponse> logs = logService.getLogsByDate(date);
            ApiResponse<List<LogResponse>> response = ApiResponse.success(
                    "Logs retrieved successfully", logs);

            return ResponseEntity.ok(response);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", date, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error(
                    "Invalid date format. Please use YYYY-MM-DD format", null);
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid date parameter: {}", date, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error("Invalid date: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error retrieving logs for date: {}", date, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error(
                    "Failed to retrieve logs for the specified date", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/employee/{email}/date/{date}")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getLogsByEmployeeAndDate(
            @PathVariable String email,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            log.info("Request to get logs for employee: {} and date: {}", email, date);

            // Validate email parameter
            if (email == null || email.trim().isEmpty()) {
                ApiResponse<List<LogResponse>> response = ApiResponse.error("Employee email cannot be empty", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Basic email format validation
            if (!email.contains("@") || !email.contains(".")) {
                ApiResponse<List<LogResponse>> response = ApiResponse.error("Invalid email format", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Validate date is not in the future
            if (date.isAfter(LocalDate.now())) {
                ApiResponse<List<LogResponse>> response = ApiResponse.error("Date cannot be in the future", null);
                return ResponseEntity.badRequest().body(response);
            }

            List<LogResponse> logs = logService.getLogsByEmployeeAndDate(email, date);
            ApiResponse<List<LogResponse>> response = ApiResponse.success(
                    "Logs retrieved successfully", logs);

            return ResponseEntity.ok(response);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format for employee: {} and date: {}", email, date, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error(
                    "Invalid date format. Please use YYYY-MM-DD format", null);
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for employee: {} and date: {}", email, date, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error("Invalid parameters: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error retrieving logs for employee: {} and date: {}", email, date, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error(
                    "Failed to retrieve logs for employee and date", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getRecentLogs(@RequestParam(defaultValue = "24") int hours) {
        try {
            log.info("Request to get recent logs from last {} hours", hours);

            // Validate hours parameter
            if (hours <= 0) {
                ApiResponse<List<LogResponse>> response = ApiResponse.error("Hours must be a positive number", null);
                return ResponseEntity.badRequest().body(response);
            }

            if (hours > 8760) { // More than a year
                ApiResponse<List<LogResponse>> response = ApiResponse.error("Hours cannot exceed 8760 (1 year)", null);
                return ResponseEntity.badRequest().body(response);
            }

            List<LogResponse> logs = logService.getRecentLogs(hours);
            ApiResponse<List<LogResponse>> response = ApiResponse.success(
                    "Recent logs retrieved successfully", logs);

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Invalid hours parameter: {}", hours, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error("Invalid hours parameter. Must be a valid number", null);
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid hours value: {}", hours, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error("Invalid hours value: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error retrieving recent logs for {} hours", hours, e);
            ApiResponse<List<LogResponse>> response = ApiResponse.error(
                    "Failed to retrieve recent logs", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}