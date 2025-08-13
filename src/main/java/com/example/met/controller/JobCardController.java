package com.example.met.controller;

import com.example.met.dto.request.RepairJobCardRequest;
import com.example.met.dto.request.ServiceJobCardRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.JobCardResponse;
import com.example.met.enums.JobCardType;
import com.example.met.service.JobCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/jobcards")
@RequiredArgsConstructor
@Slf4j
public class JobCardController {

    private final JobCardService jobCardService;

    @PostMapping("/service")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<JobCardResponse>> createServiceJobCard(@Valid @RequestBody ServiceJobCardRequest request) {
        log.info("Request to create service job card for generator: {}", request.getGeneratorId());

        JobCardResponse jobCard = jobCardService.createServiceJobCard(request);
        ApiResponse<JobCardResponse> response = ApiResponse.success("Service job card created successfully", jobCard);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/repair")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<JobCardResponse>> createRepairJobCard(@Valid @RequestBody RepairJobCardRequest request) {
        log.info("Request to create repair job card for generator: {}", request.getGeneratorId());

        JobCardResponse jobCard = jobCardService.createRepairJobCard(request);
        ApiResponse<JobCardResponse> response = ApiResponse.success("Repair job card created successfully", jobCard);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getAllJobCards() {
        log.info("Request to get all job cards");

        List<JobCardResponse> jobCards = jobCardService.getAllJobCards();
        ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                "Job cards retrieved successfully", jobCards);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobCardResponse>> getJobCardById(@PathVariable UUID id) {
        log.info("Request to get job card by ID: {}", id);

        JobCardResponse jobCard = jobCardService.getJobCardResponse(id);
        ApiResponse<JobCardResponse> response = ApiResponse.success(
                "Job card retrieved successfully", jobCard);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getJobCardsByType(@PathVariable JobCardType type) {
        log.info("Request to get job cards by type: {}", type);

        List<JobCardResponse> jobCards = jobCardService.getJobCardsByType(type);
        ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                "Job cards retrieved successfully", jobCards);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getJobCardsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Request to get job cards by date: {}", date);

        List<JobCardResponse> jobCards = jobCardService.getJobCardsByDate(date);
        ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                "Job cards for date " + date + " retrieved successfully", jobCards);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/{email}")
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getJobCardsByEmployee(@PathVariable String email) {
        log.info("Request to get job cards for employee: {}", email);

        List<JobCardResponse> jobCards = jobCardService.getJobCardsByEmployee(email);
        ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                "Job cards retrieved successfully", jobCards);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/generator/{generatorId}")
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getJobCardsByGenerator(@PathVariable UUID generatorId) {
        log.info("Request to get job cards for generator: {}", generatorId);

        List<JobCardResponse> jobCards = jobCardService.getJobCardsByGenerator(generatorId);
        ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                "Job cards retrieved successfully", jobCards);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deleteJobCard(@PathVariable UUID id) {
        log.info("Request to delete job card: {}", id);

        jobCardService.deleteJobCard(id);
        ApiResponse<Void> response = ApiResponse.success("Job card and all related tasks deleted successfully");

        return ResponseEntity.ok(response);
    }
}