package com.example.met.controller;

import com.example.met.dto.request.OTTimeReportRequest;
import com.example.met.dto.request.EmployeeTimeReportRequest;
import com.example.met.dto.response.OTTimeReportResponse;
import com.example.met.dto.response.EmployeeTimeReportResponse;
import com.example.met.service.ReportService;
import com.example.met.service.OTTimeCalculatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/reports")
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final OTTimeCalculatorService otTimeCalculatorService;

    public ReportController(ReportService reportService, OTTimeCalculatorService otTimeCalculatorService) {
        this.reportService = reportService;
        this.otTimeCalculatorService = otTimeCalculatorService;
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

    @PostMapping("/employee-ot-report")
    public ResponseEntity<OTTimeReportResponse> generateOTTimeReport(
            @Valid @RequestBody OTTimeReportRequest request) {
        try {
            log.info("Generating OT time report for employee: {} from {} to {}",
                    request.getEmployeeEmail(), request.getStartDate(), request.getEndDate());

            OTTimeReportResponse report = otTimeCalculatorService.generateOTTimeReport(request);

            log.info("Successfully generated OT report for employee: {} with {} records",
                    request.getEmployeeEmail(), report.getOtRecords().size());

            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for OT time report: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating OT time report for employee: {}", request.getEmployeeEmail(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}