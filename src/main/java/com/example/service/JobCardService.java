package com.example.service;

import com.example.dto.JobCardDTO;
import com.example.dto.LogJobCardDTO;
import com.example.entity.JobCard;
import com.example.entity.JobEventLog;
import com.example.entity.User;
import com.example.repository.JobCardRepository;
import com.example.repository.JobEventLogRepository;
import com.example.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class JobCardService {

    ZonedDateTime colomboDateTime = ZonedDateTime.now(ZoneId.of("Asia/Colombo"));

    // Format date and time separately if needed
    String currentDate = colomboDateTime.toLocalDate().toString(); // e.g. 2025-08-07
    String currentTime = colomboDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")); // e.g. 13:42:10


    @Autowired
    private JobCardRepository jobCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobEventLogRepository jobEventLogRepository;


    // Create a new Job Card
    public JobCard createJobCard(JobCardDTO jobCardDTO) {
        JobCard jobCard = new JobCard();
        jobCard.setJobid(jobCardDTO.getJobid());
        jobCard.setGeneratorid(jobCardDTO.getGeneratorid());
        jobCard.setTitle(jobCardDTO.getTitle());
        jobCard.setDescription(jobCardDTO.getDescription());
        jobCard.setHoursnumber(jobCardDTO.getHoursnumber());
        jobCard.setWorkstatus(jobCardDTO.getWorkstatus() != null ? jobCardDTO.getWorkstatus() : "Pending");
        jobCard.setDate(LocalDate.now());

        if (jobCardDTO.getAssignTo() != null) {
            Optional<User> userOptional = userRepository.findByEmail(jobCardDTO.getAssignTo());
            userOptional.ifPresent(jobCard::setAssignTo);  // Set assignTo only if user is found
        }

        return jobCardRepository.save(jobCard);
    }


    public JobCard updateJobCardEmpoyer(String jobid, LogJobCardDTO logjobCardDTO) throws Exception {
        Optional<JobCard> optionalJobCard = jobCardRepository.findByJobid(jobid);
        if (optionalJobCard.isEmpty()) {
            throw new Exception("Job card not found");
        }
        log.info("endpoint hit | {}", optionalJobCard.get().getJobid());

        // Extract email from JWT token
        String currentEmail = getCurrentUserEmailFromJwtToken();

        // Check if the current user is an employer (role is not "employee")
        Optional<User> userOptional = userRepository.findByEmail(currentEmail);

        JobCard jobCard = optionalJobCard.get();
        if (logjobCardDTO.getWorkstatuslog() != null) {
            jobCard.setWorkstatus(logjobCardDTO.getWorkstatuslog());
        }
        jobCardRepository.save(jobCard);
        // Create and save JobEventLog if user is an employer
        JobEventLog jobEventLog = new JobEventLog();

        jobEventLog.setName(userOptional.get().getName());
        jobEventLog.setEmail(currentEmail);
        jobEventLog.setGeneratorid(optionalJobCard.get().getGeneratorid());
        jobEventLog.setWorkstatuslog(logjobCardDTO.getWorkstatuslog());
        jobEventLog.setJobid(jobid);
        jobEventLog.setLocation(logjobCardDTO.getLocation());
        jobEventLog.setEventTime(LocalDateTime.now());

        jobEventLogRepository.save(jobEventLog);
        return optionalJobCard.get();
    }

    public JobCard updateJobCardAdmin(String jobid, JobCardDTO jobCardDTO) throws Exception {
        Optional<JobCard> optionalJobCard = jobCardRepository.findByJobid(jobid);
        if (optionalJobCard.isEmpty()) {
            throw new Exception("Job card not found");
        }

        JobCard jobCard = optionalJobCard.get();

        if (jobCardDTO.getTitle() != null) {
            jobCard.setTitle(jobCardDTO.getTitle());
        }
        if (jobCardDTO.getDescription() != null) {
            jobCard.setDescription(jobCardDTO.getDescription());
        }

        if (jobCardDTO.getHoursnumber() != null) {
            jobCard.setHoursnumber(jobCardDTO.getHoursnumber());
        }

        if (jobCardDTO.getWorkstatus() != null) {
            jobCard.setWorkstatus(jobCardDTO.getWorkstatus());
        }

        if (jobCardDTO.getGeneratorid() != null) {
            jobCard.setGeneratorid(jobCardDTO.getGeneratorid());
        }

        if (jobCardDTO.getAssignTo() != null && !jobCardDTO.getAssignTo().isEmpty()) {
            Optional<User> assignToUserOptional = userRepository.findByEmail(jobCardDTO.getAssignTo());
            if (assignToUserOptional.isPresent()) {
                jobCard.setAssignTo(assignToUserOptional.get());
            } else {
                throw new Exception("User not found with the given email");
            }
        }

        return jobCardRepository.save(jobCard);
    }

    // Utility method to extract the current user's email from JWT token
    private String getCurrentUserEmailFromJwtToken() {
        String email = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        }
        return email;
    }

    // Method to get all job cards
    public List<JobCard> getAllJobCards() {
        return jobCardRepository.findAll();  // Fetch all job cards without filtering
    }


    // Get Job Cards by assignTo email
    public List<JobCard> getJobCardsByAssignTo(String email) {
        return jobCardRepository.findByAssignToEmail(email);  // Query job cards by email
    }

    @Transactional
    public void deleteJobCard(String jobid) {
        // Check if there are multiple records with the same jobid
        long count = jobCardRepository.countByJobid(jobid);
        if (count == 0) {
            throw new RuntimeException("Job card with jobid " + jobid + " not found.");
        }

        // Delete all job cards with the given jobid
        jobCardRepository.deleteByJobid(jobid);
    }
}
