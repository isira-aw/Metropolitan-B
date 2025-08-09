package com.example.met.controller;

import com.example.met.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("timestamp", LocalDateTime.now());
        healthData.put("service", "Employee Management System");
        healthData.put("version", "1.0.0");

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                "Service is healthy", healthData);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        ApiResponse<String> response = ApiResponse.success("Pong", "Service is running");
        return ResponseEntity.ok(response);
    }
}