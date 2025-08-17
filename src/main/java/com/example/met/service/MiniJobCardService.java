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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MiniJobCardService {

    private final MiniJobCardRepository miniJobCardRepository;
    // REMOVED: JobCardService to break circular dependency
    private final JobCardRepository jobCardRepository;
    private final EmployeeService employeeService;
    private final LogRepository logRepository;

    @Transactional
    public MiniJobCard createMiniJobCard(MiniJobCard miniJobCard) {
        MiniJobCard saved = miniJobCardRepository.save(miniJobCard);
        log.info("Mini job card created with ID: {}", saved.getMiniJobCardId());
        return saved;
    }

    @Transactional
    public MiniJobCardResponse createMiniJobCardFromRequest(MiniJobCardRequest request) {
        log.info("Creating mini job card for job card ID: {} and employee: {}",
                request.getJobCardId(), request.getEmployeeEmail());

        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Job Card not found with id: " + request.getJobCardId()));
        Employee employee = employeeService.findByEmail(request.getEmployeeEmail());

        MiniJobCard miniJobCard = new MiniJobCard();
        miniJobCard.setJobCard(jobCard);
        miniJobCard.setEmployee(employee);
        miniJobCard.setDate(request.getDate());
        miniJobCard.setLocation(request.getLocation());
        miniJobCard.setTime(request.getTime());
        miniJobCard.setStatus(JobStatus.PENDING);

        miniJobCard = miniJobCardRepository.save(miniJobCard);
        log.info("Mini job card created successfully with ID: {}", miniJobCard.getMiniJobCardId());
        return convertToResponse(miniJobCard);
    }

    public MiniJobCard findById(UUID id) {
        return miniJobCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mini Job Card not found with id: " + id));
    }

    public MiniJobCardResponse getMiniJobCardResponse(UUID id) {
        MiniJobCard miniJobCard = findById(id);
        return convertToResponse(miniJobCard);
    }

    public List<MiniJobCardResponse> getAllMiniJobCards() {
        log.info("Fetching all mini job cards");
        return miniJobCardRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<MiniJobCardResponse> getMiniJobCardsByEmployee(String email) {
        log.info("Fetching mini job cards for employee: {}", email);
        return miniJobCardRepository.findByEmployeeEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<MiniJobCardResponse> getMiniJobCardsByEmployeeAndDate(String email, LocalDate date) {
        log.info("Fetching mini job cards for employee: {} on date: {}", email, date);
        return miniJobCardRepository.findByEmployeeEmailAndDate(email, date)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<MiniJobCardResponse> getMiniJobCardsByJobCard(UUID jobCardId) {
        log.info("Fetching mini job cards for job card: {}", jobCardId);
        return miniJobCardRepository.findByJobCardJobCardId(jobCardId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<MiniJobCardResponse> getMiniJobCardsByStatus(JobStatus status) {
        log.info("Fetching mini job cards by status: {}", status);
        return miniJobCardRepository.findByStatus(status)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MiniJobCardResponse updateMiniJobCard(UUID id, MiniJobCardUpdateRequest request) {
        log.info("Updating mini job card with ID: {}", id);

        MiniJobCard miniJobCard = findById(id);

        // Store old values for logging
        JobStatus oldStatus = miniJobCard.getStatus();
        String oldLocation = miniJobCard.getLocation();

        // Update fields
        if (request.getStatus() != null) {
            miniJobCard.setStatus(request.getStatus());
        }
        if (request.getDate() != null) {
            miniJobCard.setDate(request.getDate());
        }
        if (request.getLocation() != null) {
            miniJobCard.setLocation(request.getLocation());
        }
        if (request.getTime() != null) {
            miniJobCard.setTime(request.getTime());
        }

        miniJobCard = miniJobCardRepository.save(miniJobCard);

        // Create log entry directly
        createLogEntryDirectly(miniJobCard, oldStatus, oldLocation);

        log.info("Mini job card updated successfully with ID: {}", miniJobCard.getMiniJobCardId());
        return convertToResponse(miniJobCard);
    }

    private void createLogEntryDirectly(MiniJobCard miniJobCard, JobStatus oldStatus, String oldLocation) {
        Log log = new Log();
        log.setEmployee(miniJobCard.getEmployee());
        log.setAction("UPDATE_MINI_JOB_CARD");
        log.setDate(LocalDate.now());
        log.setTime(LocalTime.now());
        log.setStatus("Updated from " + oldStatus.name() + " to " + miniJobCard.getStatus().name());
        log.setLocation(miniJobCard.getLocation());

        logRepository.save(log);
    }



    private MiniJobCardResponse convertToResponse(MiniJobCard miniJobCard) {
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
        response.setCreatedAt(miniJobCard.getCreatedAt());
        response.setUpdatedAt(miniJobCard.getUpdatedAt());

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
    }
}