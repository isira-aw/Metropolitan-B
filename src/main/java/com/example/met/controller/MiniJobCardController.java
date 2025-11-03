package com.example.met.controller;

import com.example.met.dto.request.MiniJobCardRequest;
import com.example.met.dto.request.MiniJobCardUpdateRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.MiniJobCardResponse;
import com.example.met.enums.JobStatus;
import com.example.met.service.MiniJobCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/minijobcards")
@RequiredArgsConstructor
@Slf4j
public class MiniJobCardController {

    private final MiniJobCardService miniJobCardService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<MiniJobCardResponse>> createMiniJobCard(@Valid @RequestBody MiniJobCardRequest request) {
        try {
            log.info("Request to create mini job card for job card: {} and employee: {}",
                    request.getJobCardId(), request.getEmployeeEmail());

            MiniJobCardResponse miniJobCard = miniJobCardService.createMiniJobCardFromRequest(request);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.success("Mini job card created successfully", miniJobCard);

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request data for creating mini job card: {}", e.getMessage(), e);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.error("Invalid request data: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error creating mini job card for job card: {} and employee: {}",
                    request.getJobCardId(), request.getEmployeeEmail(), e);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.error("Failed to create mini job card", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getAllMiniJobCards() {
        try {
            log.info("Request to get today's mini job cards");

            List<MiniJobCardResponse> miniJobCards = miniJobCardService.getAllMiniJobCards();
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.success(
                    "Today's mini job cards retrieved successfully", miniJobCards);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving today's mini job cards", e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve today's mini job cards", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MiniJobCardResponse>> getMiniJobCardById(@PathVariable UUID id) {
        try {
            log.info("Request to get mini job card by ID: {}", id);

            MiniJobCardResponse miniJobCard = miniJobCardService.getMiniJobCardResponse(id);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.success(
                    "Mini job card retrieved successfully", miniJobCard);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Mini job card not found with ID: {}", id, e);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.error("Mini job card not found", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving mini job card by ID: {}", id, e);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.error(
                    "Failed to retrieve mini job card", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/employee/{email}")
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getMiniJobCardsByEmployee(@PathVariable String email) {
        try {
            log.info("Request to get mini job cards for employee: {}", email);

            // Validate email format
            if (email == null || email.trim().isEmpty()) {
                ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error("Employee email cannot be empty", null);
                return ResponseEntity.badRequest().body(response);
            }

            List<MiniJobCardResponse> miniJobCards = miniJobCardService.getMiniJobCardsByEmployee(email);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.success(
                    "Mini job cards retrieved successfully", miniJobCards);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid employee email: {}", email, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error("Invalid employee email: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error retrieving mini job cards for employee: {}", email, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve mini job cards for employee", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/employee/{email}/date/{date}")
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getMiniJobCardsByEmployeeAndDate(
            @PathVariable String email,
            @PathVariable String date) {
        try {
            log.info("Request to get mini job cards for employee: {} on date: {}", email, date);

            // Validate email
            if (email == null || email.trim().isEmpty()) {
                ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error("Employee email cannot be empty", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Parse the date string to LocalDate
            LocalDate searchDate = LocalDate.parse(date);

            List<MiniJobCardResponse> miniJobCards = miniJobCardService.getMiniJobCardsByEmployeeAndDate(email, searchDate);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.success(
                    "Mini job cards retrieved successfully for date: " + date, miniJobCards);

            return ResponseEntity.ok(response);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format: {}", date, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error(
                    "Invalid date format. Please use YYYY-MM-DD format", null);
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters for employee: {} and date: {}", email, date, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error(
                    "Invalid request parameters: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error fetching mini job cards for employee: {} on date: {}", email, date, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve mini job cards", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/jobcard/{jobCardId}")
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getMiniJobCardsByJobCard(@PathVariable UUID jobCardId) {
        try {
            log.info("Request to get mini job cards for job card: {}", jobCardId);

            List<MiniJobCardResponse> miniJobCards = miniJobCardService.getMiniJobCardsByJobCard(jobCardId);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.success(
                    "Mini job cards retrieved successfully", miniJobCards);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Job card not found with ID: {}", jobCardId, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error("Job card not found", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving mini job cards for job card: {}", jobCardId, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve mini job cards for job card", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getMiniJobCardsByStatus(@PathVariable JobStatus status) {
        try {
            log.info("Request to get mini job cards by status: {}", status);

            List<MiniJobCardResponse> miniJobCards = miniJobCardService.getMiniJobCardsByStatus(status);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.success(
                    "Mini job cards retrieved successfully", miniJobCards);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid job status: {}", status, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error("Invalid job status: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error retrieving mini job cards by status: {}", status, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve mini job cards by status", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<MiniJobCardResponse>> updateMiniJobCard(
            @PathVariable UUID id,
            @Valid @RequestBody MiniJobCardUpdateRequest request) {
        try {
            log.info("Request to update mini job card: {}", id);

            MiniJobCardResponse updatedMiniJobCard = miniJobCardService.updateMiniJobCard(id, request);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.success(
                    "Mini job card updated successfully", updatedMiniJobCard);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for updating mini job card with ID: {}", id, e);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.error("Invalid request: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            log.error("Mini job card not found for update with ID: {}", id, e);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.error("Mini job card not found", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error updating mini job card with ID: {}", id, e);
            ApiResponse<MiniJobCardResponse> response = ApiResponse.error(
                    "Failed to update mini job card", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/can-edit-status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<Boolean>> canEditStatus(Authentication authentication) {
        try {
            String employeeEmail = authentication.getName(); // Gets logged-in user's email
            log.info("Request to check if employee {} can edit status", employeeEmail);

            boolean eligible = miniJobCardService.canEmployeeEditStatus(employeeEmail);

            String message = eligible
                    ? "You can edit status for today"
                    : "You cannot edit status - either day has ended or no active session";

            ApiResponse<Boolean> response = ApiResponse.success(message, eligible);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking edit eligibility for authenticated employee", e);
            ApiResponse<Boolean> response = ApiResponse.error("Failed to check edit eligibility", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}