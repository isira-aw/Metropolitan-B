package com.example.met.service;

import com.example.met.dto.request.RepairJobCardRequest;
import com.example.met.dto.request.ServiceJobCardRequest;
import com.example.met.dto.response.JobCardResponse;
import com.example.met.entity.Employee;
import com.example.met.entity.Generator;
import com.example.met.entity.JobCard;
import com.example.met.entity.MiniJobCard;
import com.example.met.enums.JobCardType;
import com.example.met.enums.JobStatus;
import com.example.met.exception.ResourceNotFoundException;
import com.example.met.repository.JobCardRepository;
import com.example.met.repository.MiniJobCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobCardService {

    private final JobCardRepository jobCardRepository;
    private final GeneratorService generatorService;
    private final EmployeeService employeeService;
    private final MiniJobCardRepository miniJobCardRepository;

    // Sri Lanka timezone constant
    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");

    @Transactional
    public JobCardResponse createServiceJobCard(ServiceJobCardRequest request) {
        try {
            log.info("Creating service job card for generator ID: {}", request.getGeneratorId());

            // Validate request
            validateServiceJobCardRequest(request);

            Generator generator;
            try {
                generator = generatorService.findById(request.getGeneratorId());
            } catch (Exception e) {
                log.error("Error finding generator with ID: {}", request.getGeneratorId(), e);
                throw new IllegalArgumentException("Generator not found with ID: " + request.getGeneratorId(), e);
            }

            JobCard jobCard = new JobCard();
            jobCard.setGenerator(generator);
            jobCard.setJobType(JobCardType.SERVICE);
            jobCard.setDate(request.getDate());
            jobCard.setEstimatedTime(request.getEstimatedTime());
            jobCard.setEmployeeEmails(request.getEmployeeEmails());

            jobCard = jobCardRepository.save(jobCard);

            // Create mini job cards directly here with error handling
            createMiniJobCardsDirectly(jobCard, request.getEmployeeEmails());

            log.info("Service job card created successfully with ID: {}", jobCard.getJobCardId());
            return convertToResponse(jobCard);
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating service job card", e);
            throw new IllegalArgumentException("Data integrity violation: duplicate or invalid references", e);
        } catch (DataAccessException e) {
            log.error("Database error while creating service job card", e);
            throw new RuntimeException("Database error occurred while creating service job card", e);
        } catch (Exception e) {
            log.error("Unexpected error while creating service job card for generator: {}", request.getGeneratorId(), e);
            throw new RuntimeException("Failed to create service job card: " + e.getMessage(), e);
        }
    }

    @Transactional
    public JobCardResponse createRepairJobCard(RepairJobCardRequest request) {
        try {
            log.info("Creating repair job card for generator ID: {}", request.getGeneratorId());

            // Validate request
            validateRepairJobCardRequest(request);

            Generator generator;
            try {
                generator = generatorService.findById(request.getGeneratorId());
            } catch (Exception e) {
                log.error("Error finding generator with ID: {}", request.getGeneratorId(), e);
                throw new IllegalArgumentException("Generator not found with ID: " + request.getGeneratorId(), e);
            }

            JobCard jobCard = new JobCard();
            jobCard.setGenerator(generator);
            jobCard.setJobType(JobCardType.REPAIR);
            jobCard.setDate(request.getDate());
            jobCard.setEstimatedTime(request.getEstimatedTime());
            jobCard.setEmployeeEmails(request.getEmployeeEmails());

            jobCard = jobCardRepository.save(jobCard);

            // Create mini job cards directly here with error handling
            createMiniJobCardsDirectly(jobCard, request.getEmployeeEmails());

            log.info("Repair job card created successfully with ID: {}", jobCard.getJobCardId());
            return convertToResponse(jobCard);
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating repair job card", e);
            throw new IllegalArgumentException("Data integrity violation: duplicate or invalid references", e);
        } catch (DataAccessException e) {
            log.error("Database error while creating repair job card", e);
            throw new RuntimeException("Database error occurred while creating repair job card", e);
        } catch (Exception e) {
            log.error("Unexpected error while creating repair job card for generator: {}", request.getGeneratorId(), e);
            throw new RuntimeException("Failed to create repair job card: " + e.getMessage(), e);
        }
    }

    private void createMiniJobCardsDirectly(JobCard jobCard, List<String> employeeEmails) {
        if (employeeEmails != null && !employeeEmails.isEmpty()) {
            List<String> failedEmails = new ArrayList<>();

            for (String email : employeeEmails) {
                try {
                    if (email == null || email.trim().isEmpty()) {
                        log.warn("Skipping null or empty email in job card: {}", jobCard.getJobCardId());
                        continue;
                    }

                    Employee employee = employeeService.findByEmail(email.trim());

                    MiniJobCard miniJobCard = new MiniJobCard();
                    miniJobCard.setJobCard(jobCard);
                    miniJobCard.setEmployee(employee);
                    miniJobCard.setDate(jobCard.getDate());
                    miniJobCard.setStatus(JobStatus.PENDING);

                    // Safe time setting
                    try {
                        miniJobCard.setTime(LocalTime.now(SRI_LANKA_ZONE).withNano(0));
                    } catch (DateTimeException e) {
                        log.warn("Error setting current time for mini job card, using system default: {}", e.getMessage());
                        miniJobCard.setTime(LocalTime.now().withNano(0));
                    }

                    miniJobCardRepository.save(miniJobCard);
                    log.info("Mini job card created for employee: {}", email);
                } catch (ResourceNotFoundException e) {
                    log.warn("Employee not found with email: {}, skipping mini job card creation", email);
                    failedEmails.add(email);
                } catch (DataAccessException e) {
                    log.error("Database error while creating mini job card for employee: {}", email, e);
                    failedEmails.add(email);
                } catch (Exception e) {
                    log.error("Unexpected error while creating mini job card for employee: {}", email, e);
                    failedEmails.add(email);
                }
            }

            if (!failedEmails.isEmpty()) {
                log.warn("Failed to create mini job cards for {} employees: {}", failedEmails.size(), failedEmails);
                // Don't fail the entire job card creation, just log the failures
            }
        }
    }

    public JobCard findById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Job card ID cannot be null");
            }

            return jobCardRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Job Card not found with id: " + id));
        } catch (DataAccessException e) {
            log.error("Database error while finding job card by ID: {}", id, e);
            throw new RuntimeException("Database error occurred while retrieving job card", e);
        }
    }

    public JobCardResponse getJobCardResponse(UUID id) {
        try {
            JobCard jobCard = findById(id);
            return convertToResponse(jobCard);
        } catch (Exception e) {
            log.error("Error converting job card to response for ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve job card response", e);
        }
    }

    public List<JobCardResponse> getAllJobCards() {
        try {
            log.info("Fetching latest 70 job cards with pagination");
            Pageable pageable = PageRequest.of(0, 70);
            return jobCardRepository.findTop100ByOrderByUpdatedAtDesc(pageable)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Database error while fetching all job cards", e);
            throw new RuntimeException("Database error occurred while retrieving job cards", e);
        } catch (Exception e) {
            log.error("Error fetching all job cards", e);
            throw new RuntimeException("Failed to retrieve job cards", e);
        }
    }

    public List<JobCardResponse> getJobCardsByType(JobCardType type) {
        try {
            log.info("Fetching job cards by type: {}", type);

            if (type == null) {
                throw new IllegalArgumentException("Job card type cannot be null");
            }

            return jobCardRepository.findByJobType(type)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching job cards by type: {}", type, e);
            throw new RuntimeException("Database error occurred while retrieving job cards by type", e);
        } catch (Exception e) {
            log.error("Error fetching job cards by type: {}", type, e);
            throw new RuntimeException("Failed to retrieve job cards by type", e);
        }
    }

    public List<JobCardResponse> getJobCardsByEmployee(String email) {
        try {
            log.info("Fetching job cards for employee: {}", email);

            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Employee email cannot be null or empty");
            }

            // Basic email format validation
            if (!email.contains("@") || !email.contains(".")) {
                throw new IllegalArgumentException("Invalid email format");
            }

            return jobCardRepository.findByEmployeeEmail(email.trim())
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching job cards for employee: {}", email, e);
            throw new RuntimeException("Database error occurred while retrieving job cards for employee", e);
        } catch (Exception e) {
            log.error("Error fetching job cards for employee: {}", email, e);
            throw new RuntimeException("Failed to retrieve job cards for employee", e);
        }
    }

    public List<JobCardResponse> getJobCardsByDate(LocalDate date) {
        try {
            log.info("Fetching job cards by date: {}", date);

            if (date == null) {
                throw new IllegalArgumentException("Date cannot be null");
            }

            // Optional: Validate date is not too far in the future
            if (date.isAfter(LocalDate.now().plusDays(365))) {
                throw new IllegalArgumentException("Date cannot be more than 1 year in the future");
            }

            return jobCardRepository.findByDate(date)
                    .stream()
                    .sorted((j1, j2) -> j2.getUpdatedAt().compareTo(j1.getUpdatedAt())) // Latest updates first
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching job cards by date: {}", date, e);
            throw new RuntimeException("Database error occurred while retrieving job cards by date", e);
        } catch (Exception e) {
            log.error("Error fetching job cards by date: {}", date, e);
            throw new RuntimeException("Failed to retrieve job cards by date", e);
        }
    }

    public List<JobCardResponse> getJobCardsByGenerator(UUID generatorId) {
        try {
            log.info("Fetching job cards for generator: {}", generatorId);

            if (generatorId == null) {
                throw new IllegalArgumentException("Generator ID cannot be null");
            }

            return jobCardRepository.findByGeneratorGeneratorId(generatorId)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching job cards for generator: {}", generatorId, e);
            throw new RuntimeException("Database error occurred while retrieving job cards for generator", e);
        } catch (Exception e) {
            log.error("Error fetching job cards for generator: {}", generatorId, e);
            throw new RuntimeException("Failed to retrieve job cards for generator", e);
        }
    }

    private JobCardResponse convertToResponse(JobCard jobCard) {
        try {
            if (jobCard == null) {
                throw new IllegalArgumentException("Job card cannot be null");
            }

            JobCardResponse response = new JobCardResponse();
            response.setJobCardId(jobCard.getJobCardId());
            response.setJobId(jobCard.getJobId());

            // Safe generator response conversion
            try {
                response.setGenerator(generatorService.getGeneratorResponse(jobCard.getGenerator().getGeneratorId()));
            } catch (Exception e) {
                log.error("Error getting generator response for job card: {}", jobCard.getJobCardId(), e);
                throw new RuntimeException("Error retrieving generator information", e);
            }

            response.setJobType(jobCard.getJobType());
            response.setDate(jobCard.getDate());
            response.setEstimatedTime(jobCard.getEstimatedTime());
            response.setEmployeeEmails(jobCard.getEmployeeEmails());

            // Get assigned employees details safely
            if (jobCard.getEmployeeEmails() != null && !jobCard.getEmployeeEmails().isEmpty()) {
                try {
                    response.setAssignedEmployees(employeeService.getEmployeesByEmails(jobCard.getEmployeeEmails()));
                } catch (Exception e) {
                    log.error("Error getting assigned employees for job card: {}", jobCard.getJobCardId(), e);
                    // Don't fail the entire response, just log the error
                    response.setAssignedEmployees(new ArrayList<>());
                }
            }

            response.setCreatedAt(jobCard.getCreatedAt());
            response.setUpdatedAt(jobCard.getUpdatedAt());
            return response;
        } catch (Exception e) {
            log.error("Error converting job card to response", e);
            throw new RuntimeException("Failed to convert job card to response", e);
        }
    }

    @Transactional
    public void deleteJobCard(UUID id) {
        try {
            log.info("Deleting job card with ID: {}", id);

            if (id == null) {
                throw new IllegalArgumentException("Job card ID cannot be null");
            }

            JobCard jobCard = findById(id);

            // First delete all related mini job cards
            try {
                List<MiniJobCard> miniJobCards = miniJobCardRepository.findByJobCardJobCardId(id);
                if (!miniJobCards.isEmpty()) {
                    miniJobCardRepository.deleteAll(miniJobCards);
                    log.info("Deleted {} mini job cards for job card: {}", miniJobCards.size(), id);
                }
            } catch (DataAccessException e) {
                log.error("Error deleting mini job cards for job card: {}", id, e);
                throw new RuntimeException("Error deleting related mini job cards", e);
            }

            // Then delete the main job card
            try {
                jobCardRepository.delete(jobCard);
                log.info("Job card deleted successfully with ID: {}", id);
            } catch (DataAccessException e) {
                log.error("Error deleting job card: {}", id, e);
                throw new RuntimeException("Error deleting job card from database", e);
            }
        } catch (IllegalArgumentException | ResourceNotFoundException e) {
            // Re-throw these as they are already properly handled
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while deleting job card: {}", id, e);
            throw new IllegalArgumentException("Cannot delete job card due to data integrity constraints", e);
        } catch (DataAccessException e) {
            log.error("Database error while deleting job card: {}", id, e);
            throw new RuntimeException("Database error occurred while deleting job card", e);
        } catch (Exception e) {
            log.error("Unexpected error while deleting job card: {}", id, e);
            throw new RuntimeException("Failed to delete job card: " + e.getMessage(), e);
        }
    }

    // Validation methods
    private void validateServiceJobCardRequest(ServiceJobCardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Service job card request cannot be null");
        }
        if (request.getGeneratorId() == null) {
            throw new IllegalArgumentException("Generator ID cannot be null");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (request.getEmployeeEmails() == null || request.getEmployeeEmails().isEmpty()) {
            throw new IllegalArgumentException("At least one employee email is required");
        }

        // Validate employee emails
        validateEmployeeEmails(request.getEmployeeEmails());
    }

    private void validateRepairJobCardRequest(RepairJobCardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Repair job card request cannot be null");
        }
        if (request.getGeneratorId() == null) {
            throw new IllegalArgumentException("Generator ID cannot be null");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (request.getEmployeeEmails() == null || request.getEmployeeEmails().isEmpty()) {
            throw new IllegalArgumentException("At least one employee email is required");
        }

        // Validate employee emails
        validateEmployeeEmails(request.getEmployeeEmails());
    }

    private void validateEmployeeEmails(List<String> employeeEmails) {
        if (employeeEmails.size() > 20) { // Reasonable limit
            throw new IllegalArgumentException("Too many employees assigned (max 20)");
        }

        for (String email : employeeEmails) {
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Employee email cannot be null or empty");
            }
            if (!email.contains("@") || !email.contains(".")) {
                throw new IllegalArgumentException("Invalid email format: " + email);
            }
            if (email.length() > 254) { // Email RFC limit
                throw new IllegalArgumentException("Email too long: " + email);
            }
        }
    }
}