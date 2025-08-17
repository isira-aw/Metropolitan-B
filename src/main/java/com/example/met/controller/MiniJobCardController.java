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
        log.info("Request to create mini job card for job card: {} and employee: {}",
                request.getJobCardId(), request.getEmployeeEmail());

        MiniJobCardResponse miniJobCard = miniJobCardService.createMiniJobCardFromRequest(request);
        ApiResponse<MiniJobCardResponse> response = ApiResponse.success("Mini job card created successfully", miniJobCard);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getAllMiniJobCards() {
        log.info("Request to get all mini job cards");

        List<MiniJobCardResponse> miniJobCards = miniJobCardService.getAllMiniJobCards();
        ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.success(
                "Mini job cards retrieved successfully", miniJobCards);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MiniJobCardResponse>> getMiniJobCardById(@PathVariable UUID id) {
        log.info("Request to get mini job card by ID: {}", id);

        MiniJobCardResponse miniJobCard = miniJobCardService.getMiniJobCardResponse(id);
        ApiResponse<MiniJobCardResponse> response = ApiResponse.success(
                "Mini job card retrieved successfully", miniJobCard);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/{email}")
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getMiniJobCardsByEmployee(@PathVariable String email) {
        log.info("Request to get mini job cards for employee: {}", email);

        List<MiniJobCardResponse> miniJobCards = miniJobCardService.getMiniJobCardsByEmployee(email);
        ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.success(
                "Mini job cards retrieved successfully", miniJobCards);

        return ResponseEntity.ok(response);
    }

    // Controller Method
    @GetMapping("/employee/{email}/date/{date}")
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getMiniJobCardsByEmployeeAndDate(
            @PathVariable String email,
            @PathVariable String date) {
        log.info("Request to get mini job cards for employee: {} on date: {}", email, date);

        try {
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
        } catch (Exception e) {
            log.error("Error fetching mini job cards for employee: {} on date: {}", email, date, e);
            ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.error(
                    "Failed to retrieve mini job cards", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/jobcard/{jobCardId}")
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getMiniJobCardsByJobCard(@PathVariable UUID jobCardId) {
        log.info("Request to get mini job cards for job card: {}", jobCardId);

        List<MiniJobCardResponse> miniJobCards = miniJobCardService.getMiniJobCardsByJobCard(jobCardId);
        ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.success(
                "Mini job cards retrieved successfully", miniJobCards);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<MiniJobCardResponse>>> getMiniJobCardsByStatus(@PathVariable JobStatus status) {
        log.info("Request to get mini job cards by status: {}", status);

        List<MiniJobCardResponse> miniJobCards = miniJobCardService.getMiniJobCardsByStatus(status);
        ApiResponse<List<MiniJobCardResponse>> response = ApiResponse.success(
                "Mini job cards retrieved successfully", miniJobCards);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<MiniJobCardResponse>> updateMiniJobCard(
            @PathVariable UUID id,
            @Valid @RequestBody MiniJobCardUpdateRequest request) {
        log.info("Request to update mini job card: {}", id);

        MiniJobCardResponse updatedMiniJobCard = miniJobCardService.updateMiniJobCard(id, request);
        ApiResponse<MiniJobCardResponse> response = ApiResponse.success(
                "Mini job card updated successfully", updatedMiniJobCard);

        return ResponseEntity.ok(response);
    }
}