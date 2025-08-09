package com.example.met.controller;

import com.example.met.dto.request.GeneratorRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.GeneratorResponse;
import com.example.met.service.GeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        log.info("Request to create generator: {}", request.getName());

        GeneratorResponse generator = generatorService.createGenerator(request);
        ApiResponse<GeneratorResponse> response = ApiResponse.success("Generator created successfully", generator);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GeneratorResponse>>> getAllGenerators() {
        log.info("Request to get all generators");

        List<GeneratorResponse> generators = generatorService.getAllGenerators();
        ApiResponse<List<GeneratorResponse>> response = ApiResponse.success(
                "Generators retrieved successfully", generators);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GeneratorResponse>> getGeneratorById(@PathVariable UUID id) {
        log.info("Request to get generator by ID: {}", id);

        GeneratorResponse generator = generatorService.getGeneratorResponse(id);
        ApiResponse<GeneratorResponse> response = ApiResponse.success(
                "Generator retrieved successfully", generator);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GeneratorResponse>>> searchGenerators(@RequestParam String name) {
        log.info("Request to search generators by name: {}", name);

        List<GeneratorResponse> generators = generatorService.searchGeneratorsByName(name);
        ApiResponse<List<GeneratorResponse>> response = ApiResponse.success(
                "Generators found successfully", generators);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GeneratorResponse>> updateGenerator(
            @PathVariable UUID id,
            @Valid @RequestBody GeneratorRequest request) {
        log.info("Request to update generator: {}", id);

        GeneratorResponse updatedGenerator = generatorService.updateGenerator(id, request);
        ApiResponse<GeneratorResponse> response = ApiResponse.success(
                "Generator updated successfully", updatedGenerator);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGenerator(@PathVariable UUID id) {
        log.info("Request to delete generator: {}", id);

        generatorService.deleteGenerator(id);
        ApiResponse<Void> response = ApiResponse.success("Generator deleted successfully");

        return ResponseEntity.ok(response);
    }
}