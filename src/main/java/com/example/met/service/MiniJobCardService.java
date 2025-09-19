package com.example.met.service;

import com.example.met.dto.request.MiniJobCardRequest;
import com.example.met.dto.request.MiniJobCardUpdateRequest;
import com.example.met.dto.response.MiniJobCardResponse;
import com.example.met.entity.*;
import com.example.met.enums.JobStatus;
import com.example.met.exception.ResourceNotFoundException;
import com.example.met.repository.JobCardRepository;
import com.example.met.repository.LogRepository;
import com.example.met.repository.MiniJobCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MiniJobCardService {

    private final MiniJobCardRepository miniJobCardRepository;
    private final JobCardRepository jobCardRepository;
    private final EmployeeService employeeService;
    private final LogRepository logRepository;
    private final OTTimeCalculatorService otTimeCalculatorService;
    // Sri Lanka timezone constant
    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");

    @Transactional
    public MiniJobCard createMiniJobCard(MiniJobCard miniJobCard) {
        try {
            log.info("Creating mini job card for job card: {} and employee: {}",
                    miniJobCard.getJobCard().getJobCardId(),
                    miniJobCard.getEmployee().getEmail());

            // Ensure time is set if not provided with proper timezone handling
            if (miniJobCard.getTime() == null) {
                try {
                    miniJobCard.setTime(getSafeCurrentTime());
                } catch (DateTimeException e) {
                    log.warn("Error setting current time, using fallback: {}", e.getMessage());
                    miniJobCard.setTime(LocalTime.of(12, 0)); // Safe fallback
                }
            }

            if (miniJobCard.getDate() == null) {
                try {
                    miniJobCard.setDate(LocalDate.now(SRI_LANKA_ZONE));
                } catch (DateTimeException e) {
                    log.warn("Error setting current date, using fallback: {}", e.getMessage());
                    miniJobCard.setDate(LocalDate.now()); // System default fallback
                }
            }

            // Set initial timestamp for tracking
            miniJobCard.setLastTimeUpdateThisTicket(getSafeCurrentDateTime());

            // Validate required fields
            validateMiniJobCard(miniJobCard);

            MiniJobCard saved = miniJobCardRepository.save(miniJobCard);
            log.info("Mini job card created with ID: {}", saved.getMiniJobCardId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating mini job card", e);
            throw new IllegalArgumentException("Data integrity violation: " + e.getMessage(), e);
        } catch (DataAccessException e) {
            log.error("Database error while creating mini job card", e);
            throw new RuntimeException("Database error occurred while creating mini job card", e);
        } catch (Exception e) {
            log.error("Unexpected error while creating mini job card", e);
            throw new RuntimeException("Failed to create mini job card: " + e.getMessage(), e);
        }
    }

    @Transactional
    public MiniJobCardResponse createMiniJobCardFromRequest(MiniJobCardRequest request) {
        try {
            log.info("Creating mini job card for job card ID: {} and employee: {}",
                    request.getJobCardId(), request.getEmployeeEmail());

            // Validate request
            validateMiniJobCardRequest(request);

            JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job Card not found with id: " + request.getJobCardId()));

            Employee employee;
            try {
                employee = employeeService.findByEmail(request.getEmployeeEmail());
            } catch (Exception e) {
                log.error("Error finding employee with email: {}", request.getEmployeeEmail(), e);
                throw new IllegalArgumentException("Employee not found with email: " + request.getEmployeeEmail(), e);
            }

            MiniJobCard miniJobCard = new MiniJobCard();
            miniJobCard.setJobCard(jobCard);
            miniJobCard.setEmployee(employee);
            miniJobCard.setDate(request.getDate());
            miniJobCard.setLocation(request.getLocation());

            // Safe time setting with proper timezone handling
            if (request.getTime() != null) {
                try {
                    miniJobCard.setTime(request.getTime());
                } catch (DateTimeException e) {
                    log.warn("Invalid time in request, using current time: {}", e.getMessage());
                    miniJobCard.setTime(getSafeCurrentTime());
                }
            } else {
                miniJobCard.setTime(getSafeCurrentTime());
            }

            miniJobCard.setStatus(JobStatus.PENDING);

            // Set initial tracking timestamp
            miniJobCard.setLastTimeUpdateThisTicket(getSafeCurrentDateTime());

            miniJobCard = miniJobCardRepository.save(miniJobCard);
            log.info("Mini job card created successfully with ID: {}", miniJobCard.getMiniJobCardId());
            return convertToResponse(miniJobCard);
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            // Re-throw these as they are already properly handled
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating mini job card from request", e);
            throw new IllegalArgumentException("Duplicate mini job card or invalid data references", e);
        } catch (DataAccessException e) {
            log.error("Database error while creating mini job card from request", e);
            throw new RuntimeException("Database error occurred while creating mini job card", e);
        } catch (Exception e) {
            log.error("Unexpected error while creating mini job card from request", e);
            throw new RuntimeException("Failed to create mini job card: " + e.getMessage(), e);
        }
    }

    public MiniJobCard findById(UUID id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Mini job card ID cannot be null");
            }

            return miniJobCardRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Mini Job Card not found with id: " + id));
        } catch (DataAccessException e) {
            log.error("Database error while finding mini job card by ID: {}", id, e);
            throw new RuntimeException("Database error occurred while retrieving mini job card", e);
        }
    }

    public MiniJobCardResponse getMiniJobCardResponse(UUID id) {
        try {
            MiniJobCard miniJobCard = findById(id);
            return convertToResponse(miniJobCard);
        } catch (Exception e) {
            log.error("Error converting mini job card to response for ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve mini job card response", e);
        }
    }

    public List<MiniJobCardResponse> getAllMiniJobCards() {
        try {
            log.info("Fetching all mini job cards for today");
            LocalDate today = LocalDate.now(SRI_LANKA_ZONE);

            return miniJobCardRepository.findByDate(today)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Database error while fetching today's mini job cards", e);
            throw new RuntimeException("Database error occurred while retrieving today's mini job cards", e);
        } catch (Exception e) {
            log.error("Error fetching today's mini job cards", e);
            throw new RuntimeException("Failed to retrieve today's mini job cards", e);
        }
    }

    public List<MiniJobCardResponse> getMiniJobCardsByEmployee(String email) {
        try {
            log.info("Fetching mini job cards for employee: {}", email);

            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Employee email cannot be null or empty");
            }

            return miniJobCardRepository.findByEmployeeEmailOrderByCreatedAtDesc(email)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching mini job cards for employee: {}", email, e);
            throw new RuntimeException("Database error occurred while retrieving mini job cards for employee", e);
        } catch (Exception e) {
            log.error("Error fetching mini job cards for employee: {}", email, e);
            throw new RuntimeException("Failed to retrieve mini job cards for employee", e);
        }
    }

    public List<MiniJobCardResponse> getMiniJobCardsByEmployeeAndDate(String email, LocalDate date) {
        try {
            log.info("Fetching mini job cards for employee: {} on date: {}", email, date);

            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Employee email cannot be null or empty");
            }

            if (date == null) {
                throw new IllegalArgumentException("Date cannot be null");
            }

            return miniJobCardRepository.findByEmployeeEmailAndDate(email, date)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching mini job cards for employee: {} and date: {}", email, date, e);
            throw new RuntimeException("Database error occurred while retrieving mini job cards", e);
        } catch (Exception e) {
            log.error("Error fetching mini job cards for employee: {} and date: {}", email, date, e);
            throw new RuntimeException("Failed to retrieve mini job cards for employee and date", e);
        }
    }

    public List<MiniJobCardResponse> getMiniJobCardsByJobCard(UUID jobCardId) {
        try {
            log.info("Fetching mini job cards for job card: {}", jobCardId);

            if (jobCardId == null) {
                throw new IllegalArgumentException("Job card ID cannot be null");
            }

            return miniJobCardRepository.findByJobCardJobCardId(jobCardId)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching mini job cards for job card: {}", jobCardId, e);
            throw new RuntimeException("Database error occurred while retrieving mini job cards for job card", e);
        } catch (Exception e) {
            log.error("Error fetching mini job cards for job card: {}", jobCardId, e);
            throw new RuntimeException("Failed to retrieve mini job cards for job card", e);
        }
    }

    public List<MiniJobCardResponse> getMiniJobCardsByStatus(JobStatus status) {
        try {
            log.info("Fetching mini job cards by status: {}", status);

            if (status == null) {
                throw new IllegalArgumentException("Job status cannot be null");
            }

            return miniJobCardRepository.findByStatus(status)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e; // Re-throw validation errors
        } catch (DataAccessException e) {
            log.error("Database error while fetching mini job cards by status: {}", status, e);
            throw new RuntimeException("Database error occurred while retrieving mini job cards by status", e);
        } catch (Exception e) {
            log.error("Error fetching mini job cards by status: {}", status, e);
            throw new RuntimeException("Failed to retrieve mini job cards by status", e);
        }
    }

    @Transactional
    public MiniJobCardResponse updateMiniJobCard(UUID id, MiniJobCardUpdateRequest request) {
        try {
            log.info("Updating mini job card with ID: {}", id);

            if (id == null) {
                throw new IllegalArgumentException("Mini job card ID cannot be null");
            }

            if (request == null) {
                throw new IllegalArgumentException("Update request cannot be null");
            }

            MiniJobCard miniJobCard = findById(id);

            // Store old values for logging and time calculation
            JobStatus oldStatus = miniJobCard.getStatus();
            LocalDateTime lastUpdateTime = miniJobCard.getLastTimeUpdateThisTicket();

            // Get current time for calculations
            LocalDateTime currentTime = getSafeCurrentDateTime();
            JobStatus newStatus = request.getStatus();

            // Handle same status gracefully instead of throwing exception
            if (newStatus != null && oldStatus == newStatus) {
                log.info("Status update requested for mini job card {} with same status: {}. " +
                                "This might be a duplicate request or UI refresh. Returning existing card.",
                        id, oldStatus);

                // Update the timestamp to show the request was processed
                miniJobCard.setUpdatedAt(currentTime);

                // Save and return the existing card
                miniJobCard = miniJobCardRepository.save(miniJobCard);
                return convertToResponse(miniJobCard);
            }

            // Calculate time spent in previous status and update accumulation fields
            if (lastUpdateTime != null && newStatus != null) {
                long minutesSpentInPreviousStatus = ChronoUnit.MINUTES.between(lastUpdateTime, currentTime);

                // Only track time for trackable statuses and ensure non-negative time
                if (minutesSpentInPreviousStatus > 0) {
                    updateTimeSpentInStatus(miniJobCard, oldStatus, minutesSpentInPreviousStatus);
                }
            }

            // Update fields with validation
            if (request.getStatus() != null) {
                miniJobCard.setStatus(request.getStatus());
                // Update tracking timestamp only when status changes
                miniJobCard.setLastTimeUpdateThisTicket(currentTime);
            }

            if (request.getDate() != null) {
                try {
                    miniJobCard.setDate(request.getDate());
                } catch (DateTimeException e) {
                    log.error("Invalid date in update request: {}", request.getDate(), e);
                    throw new IllegalArgumentException("Invalid date format in request", e);
                }
            }

            if (request.getLocation() != null) {
                if (request.getLocation().length() > 255) { // Assuming max length
                    throw new IllegalArgumentException("Location cannot exceed 255 characters");
                }
                miniJobCard.setLocation(request.getLocation());
            }

            if (request.getTime() != null) {
                try {
                    miniJobCard.setTime(request.getTime());
                } catch (DateTimeException e) {
                    log.warn("Invalid time in request, using current time: {}", e.getMessage());
                    miniJobCard.setTime(getSafeCurrentTime());
                }
            }
            miniJobCard.setUpdatedtime(getSafeCurrentTime());

            // Save the updated mini job card
            miniJobCard = miniJobCardRepository.save(miniJobCard);

            // Create log entry safely - only for actual status changes
            if (newStatus != null && oldStatus != newStatus) {
                try {
                    MiniJobCardResponse fullResponse = convertToResponse(miniJobCard);
                    createLogEntryDirectly(miniJobCard, oldStatus, fullResponse);
                } catch (Exception e) {
                    log.error("Error creating log entry for mini job card update: {}", id, e);
                }
            }

            log.info("Mini job card updated successfully with ID: {}. Status changed from {} to {}",
                    miniJobCard.getMiniJobCardId(), oldStatus, newStatus);

            return convertToResponse(miniJobCard);

        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            // Re-throw these as they are already properly handled
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating mini job card: {}", id, e);
            throw new IllegalArgumentException("Data integrity violation during update", e);
        } catch (DataAccessException e) {
            log.error("Database error while updating mini job card: {}", id, e);
            throw new RuntimeException("Database error occurred while updating mini job card", e);
        } catch (Exception e) {
            log.error("Unexpected error while updating mini job card: {}", id, e);
            throw new RuntimeException("Failed to update mini job card: " + e.getMessage(), e);
        }
    }

    /**
     * Updates the time spent in a specific status for the mini job card
     *
     * @param miniJobCard The mini job card to update
     * @param status The status that time was spent in
     * @param minutesSpent The number of minutes spent in that status
     */
    private void updateTimeSpentInStatus(MiniJobCard miniJobCard, JobStatus status, long minutesSpent) {
        try {
            switch (status) {
                case ON_HOLD:
                    updateSpentTime(miniJobCard, "ON_HOLD", minutesSpent,
                            miniJobCard.getSpentOnOnHold(),
                            time -> miniJobCard.setSpentOnOnHold(time));
                    break;

                case ASSIGNED:
                    updateSpentTime(miniJobCard, "ASSIGNED", minutesSpent,
                            miniJobCard.getSpentOnAssigned(),
                            time -> miniJobCard.setSpentOnAssigned(time));
                    break;

                case IN_PROGRESS:
                    updateSpentTime(miniJobCard, "IN_PROGRESS", minutesSpent,
                            miniJobCard.getSpentOnInProgress(),
                            time -> miniJobCard.setSpentOnInProgress(time));
                    break;

                case PENDING:
                case COMPLETED:
                case CANCELLED:
                    // These statuses don't track time as per business requirements
                    log.debug("Status {} does not track time spent", status);
                    break;

                default:
                    log.warn("Unknown status: {}, no time tracking applied", status);
                    break;
            }
        } catch (Exception e) {
            log.error("Error updating time spent for status: {}", status, e);
            // Don't propagate this error as it's not critical for the main operation
        }
    }

    /**
     * Helper method to update spent time for a specific status
     */
    private void updateSpentTime(MiniJobCard miniJobCard, String statusName, long minutesSpent,
                                 LocalTime currentTime, java.util.function.Consumer<LocalTime> setter) {
        try {
            // Get current accumulated time or default to 00:00:00
            LocalTime accumulatedTime = currentTime != null ? currentTime : LocalTime.of(0, 0, 0);

            // Convert current time to total minutes
            int currentTotalMinutes = accumulatedTime.getHour() * 60 + accumulatedTime.getMinute();

            // Add the new minutes spent
            int newTotalMinutes = currentTotalMinutes + (int) minutesSpent;

            // Convert back to LocalTime (handle overflow properly)
            int hours = newTotalMinutes / 60;
            int minutes = newTotalMinutes % 60;

            // For time tracking, we'll allow accumulation beyond 24 hours by using modulo
            // This ensures we don't lose data but stay within LocalTime constraints
            int displayHours = hours % 24;

            // Log if time exceeds 24 hours for monitoring purposes
            if (hours >= 24) {
                log.warn("Time spent in status {} for mini job card {} exceeds 24 hours. Total: {}:{}, Display: {}:{}",
                        statusName, miniJobCard.getMiniJobCardId(), hours, minutes, displayHours, minutes);
            }

            LocalTime newTime = LocalTime.of(displayHours, minutes, 0);
            setter.accept(newTime);

            log.info("Updated {} time for mini job card {}: added {} minutes, total now: {}:{}",
                    statusName, miniJobCard.getMiniJobCardId(), minutesSpent, displayHours, minutes);

        } catch (Exception e) {
            log.error("Error calculating time for status: {}", statusName, e);
            // Set a safe default if calculation fails
            setter.accept(LocalTime.of(0, 0, 0));
        }
    }

    private void createLogEntryDirectly(MiniJobCard miniJobCard, JobStatus oldStatus, MiniJobCardResponse fullResponse) {
        try {
            Log logEntry = new Log();
            logEntry.setEmployee(miniJobCard.getEmployee());
            logEntry.setAction("UPDATE_JOB_CARD");

            // Handle OT calculation safely with detailed error handling
            try {
                if (otTimeCalculatorService != null) {
                    otTimeCalculatorService.handleFirstLog(miniJobCard);
                    log.debug("Successfully processed OT calculation for mini job card: {}", miniJobCard.getMiniJobCardId());
                } else {
                    log.warn("OT Time Calculator Service is not available");
                }
            } catch (DataIntegrityViolationException e) {
                log.error("Database constraint violation in OT calculation for mini job card: {}. Error: {}",
                        miniJobCard.getMiniJobCardId(), e.getMessage());
                // Continue with log creation even if OT calculation fails
            } catch (Exception e) {
                log.error("Failed to process OT calculation for mini job card: {}. Error: {}",
                        miniJobCard.getMiniJobCardId(), e.getMessage(), e);
                // Continue with log creation even if OT calculation fails
            }

            // Safe date and time setting
            try {
                logEntry.setDate(LocalDate.now(SRI_LANKA_ZONE));
                logEntry.setTime(getSafeCurrentTime());
            } catch (DateTimeException e) {
                logEntry.setDate(LocalDate.now());
                logEntry.setTime(LocalTime.now().withNano(0));
            }

            logEntry.setGeneratorName(fullResponse.getGeneratorName());
            logEntry.setStatus(oldStatus.name() + " to " + miniJobCard.getStatus().name());
            logEntry.setLocation(miniJobCard.getLocation());

            // Save the log entry
            logRepository.save(logEntry);
            log.info("Successfully created log entry for mini job card: {}", miniJobCard.getMiniJobCardId());

        } catch (Exception e) {
            log.error("Failed to create log entry for mini job card update: {}. Error: {}",
                    miniJobCard.getMiniJobCardId(), e.getMessage(), e);
            // Don't propagate this error as it's not critical for the main operation
        }
    }

    private MiniJobCardResponse convertToResponse(MiniJobCard miniJobCard) {
        try {
            if (miniJobCard == null) {
                throw new IllegalArgumentException("Mini job card cannot be null");
            }

            MiniJobCardResponse response = new MiniJobCardResponse();

            // Basic mini job card info
            response.setMiniJobCardId(miniJobCard.getMiniJobCardId());
            response.setJobCardId(miniJobCard.getJobCard().getJobCardId());
            response.setEmployeeEmail(miniJobCard.getEmployee().getEmail());
            response.setEmployeeName(miniJobCard.getEmployee().getName());
            response.setStatus(miniJobCard.getStatus());
            response.setDate(miniJobCard.getDate());
            response.setLocation(miniJobCard.getLocation());
            response.setTime(miniJobCard.getTime());
            response.setUpdatedTime(miniJobCard.getUpdatedtime());

            // Safe timestamp handling
            try {
                response.setCreatedAt(miniJobCard.getCreatedAt());
                response.setUpdatedAt(miniJobCard.getUpdatedAt());
            } catch (DateTimeException e) {
                log.warn("Error setting timestamps in response, using current time: {}", e.getMessage());
                LocalDateTime now = getSafeCurrentDateTime();
                response.setCreatedAt(now);
                response.setUpdatedAt(now);
            }

            // Enhanced job card details
            response.setJobType(miniJobCard.getJobCard().getJobType());
            response.setEstimatedTime(miniJobCard.getJobCard().getEstimatedTime());

            // Full generator details
            Generator generator = miniJobCard.getJobCard().getGenerator();
            response.setGeneratorId(generator.getGeneratorId());
            response.setGeneratorName(generator.getName());
            response.setGeneratorCapacity(generator.getCapacity());
            response.setGeneratorContactNumber(generator.getContactNumber());
            response.setGeneratorEmail(generator.getEmail());
            response.setGeneratorDescription(generator.getDescription());

            return response;
        } catch (Exception e) {
            log.error("Error converting mini job card to response", e);
            throw new RuntimeException("Failed to convert mini job card to response", e);
        }
    }

    // Utility methods for safe time handling
    private LocalTime getSafeCurrentTime() {
        try {
            return LocalTime.now(SRI_LANKA_ZONE).withNano(0); // Remove nanoseconds to prevent precision issues
        } catch (DateTimeException e) {
            log.warn("Error getting current time with timezone, using system default: {}", e.getMessage());
            return LocalTime.now().withNano(0); // System default without nanoseconds
        }
    }

    private LocalDateTime getSafeCurrentDateTime() {
        try {
            return LocalDateTime.now(SRI_LANKA_ZONE).withNano(0); // Remove nanoseconds
        } catch (DateTimeException e) {
            log.warn("Error getting current datetime with timezone, using system default: {}", e.getMessage());
            return LocalDateTime.now().withNano(0); // System default without nanoseconds
        }
    }

    private void validateMiniJobCard(MiniJobCard miniJobCard) {
        if (miniJobCard == null) {
            throw new IllegalArgumentException("Mini job card cannot be null");
        }
        if (miniJobCard.getJobCard() == null) {
            throw new IllegalArgumentException("Job card reference cannot be null");
        }
        if (miniJobCard.getEmployee() == null) {
            throw new IllegalArgumentException("Employee reference cannot be null");
        }
        if (miniJobCard.getDate() == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (miniJobCard.getStatus() == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    private void validateMiniJobCardRequest(MiniJobCardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getJobCardId() == null) {
            throw new IllegalArgumentException("Job card ID cannot be null");
        }
        if (request.getEmployeeEmail() == null || request.getEmployeeEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee email cannot be null or empty");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        // Email format validation
        if (!request.getEmployeeEmail().contains("@") || !request.getEmployeeEmail().contains(".")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Location length validation
        if (request.getLocation() != null && request.getLocation().length() > 255) {
            throw new IllegalArgumentException("Location cannot exceed 255 characters");
        }
    }
}