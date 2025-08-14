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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    // REMOVED: MiniJobCardService to break circular dependency
    private final MiniJobCardRepository miniJobCardRepository;

    @Transactional
    public JobCardResponse createServiceJobCard(ServiceJobCardRequest request) {
        log.info("Creating service job card for generator ID: {}", request.getGeneratorId());

        Generator generator = generatorService.findById(request.getGeneratorId());

        JobCard jobCard = new JobCard();
        jobCard.setGenerator(generator);
        jobCard.setJobType(JobCardType.SERVICE);
        jobCard.setDate(request.getDate());
        jobCard.setEstimatedTime(request.getEstimatedTime());
        jobCard.setEmployeeEmails(request.getEmployeeEmails());

        jobCard = jobCardRepository.save(jobCard);

        // Create mini job cards directly here
        createMiniJobCardsDirectly(jobCard, request.getEmployeeEmails());

        log.info("Service job card created successfully with ID: {}", jobCard.getJobCardId());
        return convertToResponse(jobCard);
    }

    @Transactional
    public JobCardResponse createRepairJobCard(RepairJobCardRequest request) {
        log.info("Creating repair job card for generator ID: {}", request.getGeneratorId());

        Generator generator = generatorService.findById(request.getGeneratorId());

        JobCard jobCard = new JobCard();
        jobCard.setGenerator(generator);
        jobCard.setJobType(JobCardType.REPAIR);
        jobCard.setDate(request.getDate());
        jobCard.setEstimatedTime(request.getEstimatedTime());
        jobCard.setEmployeeEmails(request.getEmployeeEmails());

        jobCard = jobCardRepository.save(jobCard);

        // Create mini job cards directly here
        createMiniJobCardsDirectly(jobCard, request.getEmployeeEmails());

        log.info("Repair job card created successfully with ID: {}", jobCard.getJobCardId());
        return convertToResponse(jobCard);
    }

    private void createMiniJobCardsDirectly(JobCard jobCard, List<String> employeeEmails) {
        if (employeeEmails != null && !employeeEmails.isEmpty()) {
            for (String email : employeeEmails) {
                try {
                    Employee employee = employeeService.findByEmail(email);

                    MiniJobCard miniJobCard = new MiniJobCard();
                    miniJobCard.setJobCard(jobCard);
                    miniJobCard.setEmployee(employee);
                    miniJobCard.setDate(jobCard.getDate());
                    miniJobCard.setStatus(JobStatus.PENDING);

                    miniJobCardRepository.save(miniJobCard);
                    log.info("Mini job card created for employee: {}", email);
                } catch (ResourceNotFoundException e) {
                    log.warn("Employee not found with email: {}, skipping mini job card creation", email);
                }
            }
        }
    }

    public JobCard findById(UUID id) {
        return jobCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job Card not found with id: " + id));
    }

    public JobCardResponse getJobCardResponse(UUID id) {
        JobCard jobCard = findById(id);
        return convertToResponse(jobCard);
    }

//    public List<JobCardResponse> getAllJobCards() {
//        log.info("Fetching all job cards");
//        return jobCardRepository.findAll()
//                .stream()
//                .map(this::convertToResponse)
//                .collect(Collectors.toList());
//    }
public List<JobCardResponse> getAllJobCards() {
    log.info("Fetching latest 100 job cards");
    Pageable pageable = PageRequest.of(0, 50);
    return jobCardRepository.findTop100ByOrderByUpdatedAtDesc(pageable)
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
}

    public List<JobCardResponse> getJobCardsByType(JobCardType type) {
        log.info("Fetching job cards by type: {}", type);
        return jobCardRepository.findByJobType(type)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<JobCardResponse> getJobCardsByEmployee(String email) {
        log.info("Fetching job cards for employee: {}", email);
        return jobCardRepository.findByEmployeeEmail(email)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<JobCardResponse> getJobCardsByDate(LocalDate date) {
        log.info("Fetching job cards by date: {}", date);

        return jobCardRepository.findByDate(date)
                .stream()
                .sorted((j1, j2) -> j2.getUpdatedAt().compareTo(j1.getUpdatedAt())) // Latest updates first
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<JobCardResponse> getJobCardsByGenerator(UUID generatorId) {
        log.info("Fetching job cards for generator: {}", generatorId);
        return jobCardRepository.findByGeneratorGeneratorId(generatorId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private JobCardResponse convertToResponse(JobCard jobCard) {
        JobCardResponse response = new JobCardResponse();
        response.setJobCardId(jobCard.getJobCardId());
        response.setJobId(jobCard.getJobId());
        response.setGenerator(generatorService.getGeneratorResponse(jobCard.getGenerator().getGeneratorId()));
        response.setJobType(jobCard.getJobType());
        response.setDate(jobCard.getDate());
        response.setEstimatedTime(jobCard.getEstimatedTime());
        response.setEmployeeEmails(jobCard.getEmployeeEmails());

        // Get assigned employees details
        if (jobCard.getEmployeeEmails() != null && !jobCard.getEmployeeEmails().isEmpty()) {
            response.setAssignedEmployees(employeeService.getEmployeesByEmails(jobCard.getEmployeeEmails()));
        }

        response.setCreatedAt(jobCard.getCreatedAt());
        response.setUpdatedAt(jobCard.getUpdatedAt());
        return response;
    }

    @Transactional
    public void deleteJobCard(UUID id) {
        log.info("Deleting job card with ID: {}", id);

        JobCard jobCard = findById(id);

        // First delete all related mini job cards
        List<MiniJobCard> miniJobCards = miniJobCardRepository.findByJobCardJobCardId(id);
        if (!miniJobCards.isEmpty()) {
            miniJobCardRepository.deleteAll(miniJobCards);
            log.info("Deleted {} mini job cards for job card: {}", miniJobCards.size(), id);
        }

        // Then delete the main job card
        jobCardRepository.delete(jobCard);
        log.info("Job card deleted successfully with ID: {}", id);
    }
}