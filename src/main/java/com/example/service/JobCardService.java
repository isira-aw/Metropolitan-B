package com.example.service;

import com.example.dto.JobCardDTO;
import com.example.entity.JobCard;
import com.example.entity.JobEventLog;
import com.example.entity.User;
import com.example.repository.JobCardRepository;
import com.example.repository.JobEventLogRepository;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class JobCardService {

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

        if (jobCardDTO.getAssignTo() != null) {
            Optional<User> userOptional = userRepository.findByEmail(jobCardDTO.getAssignTo());
            userOptional.ifPresent(jobCard::setAssignTo);  // Set assignTo only if user is found
        }

        return jobCardRepository.save(jobCard);
    }


    public JobCard updateJobCard(String jobid, JobCardDTO jobCardDTO) throws Exception {
        Optional<JobCard> optionalJobCard = jobCardRepository.findByJobid(jobid);
        if (optionalJobCard.isEmpty()) {
            throw new Exception("Job card not found");
        }

        // Extract email from JWT token
        String currentEmail = getCurrentUserEmailFromJwtToken();

        // Check if the current user is an employer (role is not "employee")
        Optional<User> userOptional = userRepository.findByEmail(currentEmail);

            // Create and save JobEventLog if user is an employer
            JobEventLog jobEventLog = new JobEventLog();
            jobEventLog.setName(userOptional.get().getName());
            jobEventLog.setEmail(currentEmail);
            jobEventLog.setGeneratorid(jobCardDTO.getGeneratorid());
            jobEventLog.setLocation(jobCardDTO.getLocation());
            jobEventLog.setWorkstatuslog(jobCardDTO.getWorkstatus());
            jobEventLog.setJobid(jobCardDTO.getJobid());
            jobEventLog.setEventTime(LocalDateTime.now());

            jobEventLogRepository.save(jobEventLog);


        // Proceed with updating the job card
        JobCard jobCard = optionalJobCard.get();

        // Update job card details
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

    // Get Job Cards by assignTo email
    public List<JobCard> getJobCardsByAssignTo(String email) {
        return jobCardRepository.findByAssignToEmail(email);  // Query job cards by email
    }

    // Delete Job Card by jobid
    public void deleteJobCard(String jobid) {
        jobCardRepository.deleteByJobid(jobid);  // Delete job card by jobid
    }
}
