package com.example.met.controller;

import com.example.met.dto.request.ReportRequest;
import com.example.met.dto.response.ReportDataResponse;
import com.example.met.service.ReportService;
import com.example.met.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final EmployeeService employeeService;


    @PostMapping("/employee/preview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or #request.email == authentication.name")
    public ResponseEntity<List<ReportDataResponse>> previewReportData(@Valid @RequestBody ReportRequest request) {
        log.info("Previewing report data for: {} from {} to {}",
                request.getEmail(), request.getStartDate(), request.getEndDate());

        try {
            // Validate date range
            if (request.getStartDate().isAfter(request.getEndDate())) {
                return ResponseEntity.badRequest().build();
            }

            List<ReportDataResponse> reportData = reportService.generateReportData(request);
            return ResponseEntity.ok(reportData);

        } catch (Exception e) {
            log.error("Error previewing report data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}