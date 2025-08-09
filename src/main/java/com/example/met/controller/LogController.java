package com.example.met.controller;

import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.LogResponse;
import com.example.met.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
        log.info("Request to get all logs");

        List<LogResponse> logs = logService.getAllLogs();
        ApiResponse<List<LogResponse>> response = ApiResponse.success(
                "Logs retrieved successfully", logs);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LogResponse>> getLogById(@PathVariable UUID id) {
        log.info("Request to get log by ID: {}", id);

        LogResponse logResponse = logService.getLogResponse(id);
        ApiResponse<LogResponse> response = ApiResponse.success(
                "Log retrieved successfully", logResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/{email}")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getLogsByEmployee(@PathVariable String email) {
        log.info("Request to get logs for employee: {}", email);

        List<LogResponse> logs = logService.getLogsByEmployee(email);
        ApiResponse<List<LogResponse>> response = ApiResponse.success(
                "Logs retrieved successfully", logs);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getLogsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Request to get logs for date: {}", date);

        List<LogResponse> logs = logService.getLogsByDate(date);
        ApiResponse<List<LogResponse>> response = ApiResponse.success(
                "Logs retrieved successfully", logs);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/{email}/date/{date}")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getLogsByEmployeeAndDate(
            @PathVariable String email,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Request to get logs for employee: {} and date: {}", email, date);

        List<LogResponse> logs = logService.getLogsByEmployeeAndDate(email, date);
        ApiResponse<List<LogResponse>> response = ApiResponse.success(
                "Logs retrieved successfully", logs);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<LogResponse>>> getRecentLogs(@RequestParam(defaultValue = "24") int hours) {
        log.info("Request to get recent logs from last {} hours", hours);

        List<LogResponse> logs = logService.getRecentLogs(hours);
        ApiResponse<List<LogResponse>> response = ApiResponse.success(
                "Recent logs retrieved successfully", logs);

        return ResponseEntity.ok(response);
    }
}