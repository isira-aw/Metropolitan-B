package com.example.met.service;

import com.example.met.dto.request.GeneratorRequest;
import com.example.met.dto.response.GeneratorResponse;
import com.example.met.entity.Generator;
import com.example.met.exception.ResourceNotFoundException;
import com.example.met.repository.GeneratorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratorService {

    private final GeneratorRepository generatorRepository;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Phone number validation pattern (basic)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{10,15}$"
    );

    @Transactional
    public GeneratorResponse createGenerator(GeneratorRequest request) {
        try {
            log.info("Creating new generator with name: {}", request.getName());

            // Validate request
            validateGeneratorRequest(request);

            Generator generator = new Generator();
            generator.setName(request.getName().trim());
            generator.setCapacity(request.getCapacity());
            generator.setContactNumber(request.getContactNumber().trim());
            generator.setEmail(request.getEmail().trim().toLowerCase());
            generator.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

            Generator savedGenerator = generatorRepository.save(generator);
            log.info("Generator created successfully with ID: {}", savedGenerator.getGeneratorId());
            return convertToResponse(savedGenerator);
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating generator: {}", request.getName(), e);
            if (e.getMessage().toLowerCase().contains("name")) {
                throw new IllegalArgumentException("Generator with this name already exists", e);
            } else if (e.getMessage().toLowerCase().contains("email")) {
                throw new IllegalArgumentException("Generator with this email already exists", e);
            } else {
                throw new IllegalArgumentException("Generator with this information already exists", e);
            }
        } catch (DataAccessException e) {
            log.error("Database error while creating generator: {}", request.getName(), e);
            throw new RuntimeException("Database error occurred while creating generator", e);
        } catch (Exception e) {
            log.error("Unexpected error while creating generator: {}", request.getName(), e);
            throw new RuntimeException("Failed to create generator: " + e.getMessage(), e);
        }
    }

    public Generator findById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Generator ID cannot be null");
            }

            return generatorRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Generator not found with id: " + id));
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            // Re-throw these as they are already properly handled
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while finding generator by ID: {}", id, e);
            throw new RuntimeException("Database error occurred while retrieving generator", e);
        } catch (Exception e) {
            log.error("Unexpected error while finding generator by ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve generator", e);
        }
    }

    public GeneratorResponse getGeneratorResponse(UUID id) {
        try {
            Generator generator = findById(id);
            return convertToResponse(generator);
        } catch (Exception e) {
            log.error("Error converting generator to response for ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve generator response", e);
        }
    }

    public List<GeneratorResponse> getAllGenerators() {
        try {
            log.info("Fetching all generators");
            return generatorRepository.findAllOrderByCreatedAtDesc()
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Database error while fetching all generators", e);
            throw new RuntimeException("Database error occurred while retrieving generators", e);
        } catch (Exception e) {
            log.error("Error fetching all generators", e);
            throw new RuntimeException("Failed to retrieve generators", e);
        }
    }

    public List<GeneratorResponse> searchGeneratorsByName(String name) {
        try {
            log.info("Searching generators by name: {}", name);

            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Search name cannot be null or empty");
            }

            if (name.trim().length() < 2) {
                throw new IllegalArgumentException("Search name must be at least 2 characters long");
            }

            if (name.trim().length() > 100) {
                throw new IllegalArgumentException("Search name cannot exceed 100 characters");
            }

            return generatorRepository.findByNameContaining(name.trim())
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while searching generators by name: {}", name, e);
            throw new RuntimeException("Database error occurred while searching generators", e);
        } catch (Exception e) {
            log.error("Error searching generators by name: {}", name, e);
            throw new RuntimeException("Failed to search generators", e);
        }
    }

    @Transactional
    public GeneratorResponse updateGenerator(UUID id, GeneratorRequest request) {
        try {
            log.info("Updating generator with ID: {}", id);

            if (id == null) {
                throw new IllegalArgumentException("Generator ID cannot be null");
            }

            // Validate request
            validateGeneratorRequest(request);

            Generator generator = findById(id);

            // Update fields with validation
            generator.setName(request.getName().trim());
            generator.setCapacity(request.getCapacity());
            generator.setContactNumber(request.getContactNumber().trim());
            generator.setEmail(request.getEmail().trim().toLowerCase());
            generator.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

            generator = generatorRepository.save(generator);
            log.info("Generator updated successfully with ID: {}", generator.getGeneratorId());
            return convertToResponse(generator);
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            // Re-throw these as they are already properly handled
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating generator: {}", id, e);
            if (e.getMessage().toLowerCase().contains("name")) {
                throw new IllegalArgumentException("Another generator with this name already exists", e);
            } else if (e.getMessage().toLowerCase().contains("email")) {
                throw new IllegalArgumentException("Another generator with this email already exists", e);
            } else {
                throw new IllegalArgumentException("Data integrity violation during update", e);
            }
        } catch (DataAccessException e) {
            log.error("Database error while updating generator: {}", id, e);
            throw new RuntimeException("Database error occurred while updating generator", e);
        } catch (Exception e) {
            log.error("Unexpected error while updating generator: {}", id, e);
            throw new RuntimeException("Failed to update generator: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteGenerator(UUID id) {
        try {
            log.info("Deleting generator with ID: {}", id);

            if (id == null) {
                throw new IllegalArgumentException("Generator ID cannot be null");
            }

            Generator generator = findById(id);
            generatorRepository.delete(generator);
            log.info("Generator deleted successfully with ID: {}", id);
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            // Re-throw these as they are already properly handled
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while deleting generator: {}", id, e);
            throw new IllegalArgumentException("Cannot delete generator. It is referenced by existing job cards or other records", e);
        } catch (DataAccessException e) {
            log.error("Database error while deleting generator: {}", id, e);
            throw new RuntimeException("Database error occurred while deleting generator", e);
        } catch (Exception e) {
            log.error("Unexpected error while deleting generator: {}", id, e);
            throw new RuntimeException("Failed to delete generator: " + e.getMessage(), e);
        }
    }

    private GeneratorResponse convertToResponse(Generator generator) {
        try {
            if (generator == null) {
                throw new IllegalArgumentException("Generator cannot be null");
            }

            GeneratorResponse response = new GeneratorResponse();
            response.setGeneratorId(generator.getGeneratorId());
            response.setName(generator.getName());
            response.setCapacity(generator.getCapacity());
            response.setContactNumber(generator.getContactNumber());
            response.setEmail(generator.getEmail());
            response.setDescription(generator.getDescription());
            response.setCreatedAt(generator.getCreatedAt());
            response.setUpdatedAt(generator.getUpdatedAt());
            return response;
        } catch (Exception e) {
            log.error("Error converting generator to response", e);
            throw new RuntimeException("Failed to convert generator to response", e);
        }
    }

    // Validation methods
    private void validateGeneratorRequest(GeneratorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Generator request cannot be null");
        }

        // Name validation
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Generator name cannot be null or empty");
        }
        if (request.getName().trim().length() < 2) {
            throw new IllegalArgumentException("Generator name must be at least 2 characters long");
        }
        if (request.getName().trim().length() > 100) {
            throw new IllegalArgumentException("Generator name cannot exceed 100 characters");
        }

        // Capacity validation
        if (request.getCapacity() == null) {
            throw new IllegalArgumentException("Generator capacity cannot be null");
        }

        // Contact number validation
        if (request.getContactNumber() == null || request.getContactNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Contact number cannot be null or empty");
        }
        String cleanPhone = request.getContactNumber().trim().replaceAll("[\\s\\-\\(\\)]", "");
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new IllegalArgumentException("Invalid contact number format");
        }

        // Email validation
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (request.getEmail().trim().length() > 254) { // Email RFC limit
            throw new IllegalArgumentException("Email cannot exceed 254 characters");
        }

        // Description validation (optional field)
        if (request.getDescription() != null && request.getDescription().trim().length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters");
        }
    }
}