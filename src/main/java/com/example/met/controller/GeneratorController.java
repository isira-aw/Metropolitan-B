package com.example.met.controller;

import com.example.met.dto.request.GeneratorRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.GeneratorResponse;
import com.example.met.service.GeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/generators")
@RequiredArgsConstructor
@Slf4j
public class GeneratorController {

    private final GeneratorService generatorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GeneratorResponse>> createGenerator(@Valid @RequestBody GeneratorRequest request) {
        try {
            log.info("Request to create generator: {}", request.getName());

            GeneratorResponse generator = generatorService.createGenerator(request);
            ApiResponse<GeneratorResponse> response = ApiResponse.success("Generator created successfully", generator);

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request data for creating generator: {}", e.getMessage(), e);
            ApiResponse<GeneratorResponse> response = ApiResponse.error("Invalid request data: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating generator: {}", request.getName(), e);
            ApiResponse<GeneratorResponse> response = ApiResponse.error("Generator with this name or identifier already exists", null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (SecurityException e) {
            log.error("Security error while creating generator: {}", request.getName(), e);
            ApiResponse<GeneratorResponse> response = ApiResponse.error("Access denied. Admin privileges required", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("Error creating generator: {}", request.getName(), e);
            ApiResponse<GeneratorResponse> response = ApiResponse.error("Failed to create generator", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GeneratorResponse>>> getAllGenerators() {
        try {
            log.info("Request to get all generators");

            List<GeneratorResponse> generators = generatorService.getAllGenerators();
            ApiResponse<List<GeneratorResponse>> response = ApiResponse.success(
                    "Generators retrieved successfully", generators);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving all generators", e);
            ApiResponse<List<GeneratorResponse>> response = ApiResponse.error(
                    "Failed to retrieve generators", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GeneratorResponse>> getGeneratorById(@PathVariable UUID id) {
        try {
            log.info("Request to get generator by ID: {}", id);

            GeneratorResponse generator = generatorService.getGeneratorResponse(id);
            ApiResponse<GeneratorResponse> response = ApiResponse.success(
                    "Generator retrieved successfully", generator);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Generator not found with ID: {}", id, e);
            ApiResponse<GeneratorResponse> response = ApiResponse.error("Generator not found with the provided ID", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving generator by ID: {}", id, e);
            ApiResponse<GeneratorResponse> response = ApiResponse.error(
                    "Failed to retrieve generator", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GeneratorResponse>>> searchGenerators(@RequestParam String name) {
        try {
            log.info("Request to search generators by name: {}", name);

            // Validate search parameter
            if (name == null || name.trim().isEmpty()) {
                ApiResponse<List<GeneratorResponse>> response = ApiResponse.error("Search name parameter cannot be empty", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Validate minimum search length
            if (name.trim().length() < 2) {
                ApiResponse<List<GeneratorResponse>> response = ApiResponse.error("Search name must be at least 2 characters long", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Validate maximum search length to prevent potential DoS
            if (name.trim().length() > 100) {
                ApiResponse<List<GeneratorResponse>> response = ApiResponse.error("Search name cannot exceed 100 characters", null);
                return ResponseEntity.badRequest().body(response);
            }

            List<GeneratorResponse> generators = generatorService.searchGeneratorsByName(name);
            ApiResponse<List<GeneratorResponse>> response = ApiResponse.success(
                    "Generators found successfully", generators);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid search parameter: {}", name, e);
            ApiResponse<List<GeneratorResponse>> response = ApiResponse.error("Invalid search parameter: " + e.getMessage(), null);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error searching generators by name: {}", name, e);
            ApiResponse<List<GeneratorResponse>> response = ApiResponse.error(
                    "Failed to search generators", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GeneratorResponse>> updateGenerator(
            @PathVariable UUID id,
            @Valid @RequestBody GeneratorRequest request) {
        try {
            log.info("Request to update generator: {}", id);

            GeneratorResponse updatedGenerator = generatorService.updateGenerator(id, request);
            ApiResponse<GeneratorResponse> response = ApiResponse.success(
                    "Generator updated successfully", updatedGenerator);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Generator not found for update with ID: {} or invalid request data: {}", id, e.getMessage(), e);

            // Check if it's a not found error or validation error based on message
            if (e.getMessage().toLowerCase().contains("not found") || e.getMessage().toLowerCase().contains("does not exist")) {
                ApiResponse<GeneratorResponse> response = ApiResponse.error("Generator not found with the provided ID", null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                ApiResponse<GeneratorResponse> response = ApiResponse.error("Invalid request data: " + e.getMessage(), null);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating generator with ID: {}", id, e);
            ApiResponse<GeneratorResponse> response = ApiResponse.error("Generator with this name or identifier already exists", null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (SecurityException e) {
            log.error("Security error while updating generator with ID: {}", id, e);
            ApiResponse<GeneratorResponse> response = ApiResponse.error("Access denied. Admin privileges required", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("Error updating generator with ID: {}", id, e);
            ApiResponse<GeneratorResponse> response = ApiResponse.error(
                    "Failed to update generator", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGenerator(@PathVariable UUID id) {
        try {
            log.info("Request to delete generator: {}", id);

            generatorService.deleteGenerator(id);
            ApiResponse<Void> response = ApiResponse.success("Generator deleted successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Generator not found for deletion with ID: {}", id, e);
            ApiResponse<Void> response = ApiResponse.error("Generator not found with the provided ID", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while deleting generator with ID: {}", id, e);
            ApiResponse<Void> response = ApiResponse.error("Cannot delete generator. It is referenced by existing job cards or other records", null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (SecurityException e) {
            log.error("Security error while deleting generator with ID: {}", id, e);
            ApiResponse<Void> response = ApiResponse.error("Access denied. Admin privileges required", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (RuntimeException e) {
            log.error("Business logic error while deleting generator with ID: {}", id, e);
            ApiResponse<Void> response = ApiResponse.error("Cannot delete generator: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            log.error("Error deleting generator with ID: {}", id, e);
            ApiResponse<Void> response = ApiResponse.error("Failed to delete generator", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}