package com.example.met.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import com.example.met.service.ReportService;
import com.example.met.dto.request.EmployeeTimeReportRequest;
import com.example.met.dto.response.EmployeeTimeReportResponse;

@RestController
@RequestMapping("/reports")
@Slf4j
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/employee-time-report")
    public ResponseEntity<EmployeeTimeReportResponse> generateEmployeeTimeReport(
            @Valid @RequestBody EmployeeTimeReportRequest request) {
        try {
            log.info("Generating time report for employee: {} from {} to {}",
                    request.getEmployeeEmail(), request.getStartDate(), request.getEndDate());

            EmployeeTimeReportResponse report = reportService.generateEmployeeTimeReport(request);

            log.info("Successfully generated report for employee: {} with {} job cards",
                    request.getEmployeeEmail(), report.getJobCards().size());

            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for time report: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating time report for employee: {}", request.getEmployeeEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}