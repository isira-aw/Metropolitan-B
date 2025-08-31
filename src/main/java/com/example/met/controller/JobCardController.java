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
import java.time.format.DateTimeParseException;
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
        try {
            log.info("Request to create service job card for generator: {}", request.getGeneratorId());

            JobCardResponse jobCard = jobCardService.createServiceJobCard(request);
            ApiResponse<JobCardResponse> response = ApiResponse.success("Service job card created successfully", jobCard);

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request data for creating service job card: {}", e.getMessage(), e);
            ApiResponse<JobCardResponse> response = ApiResponse.error("Invalid request data: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (SecurityException e) {
            log.error("Security error while creating service job card for generator: {}", request.getGeneratorId(), e);
            ApiResponse<JobCardResponse> response = ApiResponse.error("Access denied. Insufficient privileges", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("Error creating service job card for generator: {}", request.getGeneratorId(), e);
            ApiResponse<JobCardResponse> response = ApiResponse.error("Failed to create service job card", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/repair")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<JobCardResponse>> createRepairJobCard(@Valid @RequestBody RepairJobCardRequest request) {
        try {
            log.info("Request to create repair job card for generator: {}", request.getGeneratorId());

            JobCardResponse jobCard = jobCardService.createRepairJobCard(request);
            ApiResponse<JobCardResponse> response = ApiResponse.success("Repair job card created successfully", jobCard);

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request data for creating repair job card: {}", e.getMessage(), e);
            ApiResponse<JobCardResponse> response = ApiResponse.error("Invalid request data: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (SecurityException e) {
            log.error("Security error while creating repair job card for generator: {}", request.getGeneratorId(), e);
            ApiResponse<JobCardResponse> response = ApiResponse.error("Access denied. Insufficient privileges", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("Error creating repair job card for generator: {}", request.getGeneratorId(), e);
            ApiResponse<JobCardResponse> response = ApiResponse.error("Failed to create repair job card", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getAllJobCards() {
        try {
            log.info("Request to get all job cards");

            List<JobCardResponse> jobCards = jobCardService.getAllJobCards();
            ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                    "Job cards retrieved successfully", jobCards);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving all job cards", e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve job cards", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobCardResponse>> getJobCardById(@PathVariable UUID id) {
        try {
            log.info("Request to get job card by ID: {}", id);

            JobCardResponse jobCard = jobCardService.getJobCardResponse(id);
            ApiResponse<JobCardResponse> response = ApiResponse.success(
                    "Job card retrieved successfully", jobCard);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Job card not found with ID: {}", id, e);
            ApiResponse<JobCardResponse> response = ApiResponse.error("Job card not found with the provided ID", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving job card by ID: {}", id, e);
            ApiResponse<JobCardResponse> response = ApiResponse.error(
                    "Failed to retrieve job card", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getJobCardsByType(@PathVariable JobCardType type) {
        try {
            log.info("Request to get job cards by type: {}", type);

            List<JobCardResponse> jobCards = jobCardService.getJobCardsByType(type);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                    "Job cards retrieved successfully", jobCards);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid job card type: {}", type, e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error("Invalid job card type: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error retrieving job cards by type: {}", type, e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve job cards by type", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getJobCardsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            log.info("Request to get job cards by date: {}", date);

            // Validate date parameter
            if (date == null) {
                ApiResponse<List<JobCardResponse>> response = ApiResponse.error("Date parameter is required", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Check if date is too far in the future (optional business rule)
            if (date.isAfter(LocalDate.now().plusDays(30))) {
                ApiResponse<List<JobCardResponse>> response = ApiResponse.error("Date cannot be more than 30 days in the future", null);
                return ResponseEntity.badRequest().body(response);
            }

            List<JobCardResponse> jobCards = jobCardService.getJobCardsByDate(date);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                    "Job cards for date " + date + " retrieved successfully", jobCards);

            return ResponseEntity.ok(response);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", date, e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error(
                    "Invalid date format. Please use YYYY-MM-DD format", null);
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid date parameter: {}", date, e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error("Invalid date: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error retrieving job cards by date: {}", date, e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve job cards for the specified date", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/employee/{email}")
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getJobCardsByEmployee(@PathVariable String email) {
        try {
            log.info("Request to get job cards for employee: {}", email);

            // Validate email parameter
            if (email == null || email.trim().isEmpty()) {
                ApiResponse<List<JobCardResponse>> response = ApiResponse.error("Employee email cannot be empty", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Basic email format validation
            if (!email.contains("@") || !email.contains(".")) {
                ApiResponse<List<JobCardResponse>> response = ApiResponse.error("Invalid email format", null);
                return ResponseEntity.badRequest().body(response);
            }

            List<JobCardResponse> jobCards = jobCardService.getJobCardsByEmployee(email);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                    "Job cards retrieved successfully", jobCards);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid employee email: {}", email, e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error("Invalid employee email: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error retrieving job cards for employee: {}", email, e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve job cards for employee", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/generator/{generatorId}")
    public ResponseEntity<ApiResponse<List<JobCardResponse>>> getJobCardsByGenerator(@PathVariable UUID generatorId) {
        try {
            log.info("Request to get job cards for generator: {}", generatorId);

            List<JobCardResponse> jobCards = jobCardService.getJobCardsByGenerator(generatorId);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.success(
                    "Job cards retrieved successfully", jobCards);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Generator not found with ID: {}", generatorId, e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error("Generator not found with the provided ID", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving job cards for generator: {}", generatorId, e);
            ApiResponse<List<JobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve job cards for generator", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deleteJobCard(@PathVariable UUID id) {
        try {
            log.info("Request to delete job card: {}", id);

            jobCardService.deleteJobCard(id);
            ApiResponse<Void> response = ApiResponse.success("Job card and all related tasks deleted successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Job card not found for deletion with ID: {}", id, e);
            ApiResponse<Void> response = ApiResponse.error("Job card not found with the provided ID", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (SecurityException e) {
            log.error("Security error while deleting job card with ID: {}", id, e);
            ApiResponse<Void> response = ApiResponse.error("Access denied. Insufficient privileges to delete job card", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (RuntimeException e) {
            log.error("Business logic error while deleting job card with ID: {}", id, e);
            ApiResponse<Void> response = ApiResponse.error("Cannot delete job card: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            log.error("Error deleting job card with ID: {}", id, e);
            ApiResponse<Void> response = ApiResponse.error("Failed to delete job card", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}