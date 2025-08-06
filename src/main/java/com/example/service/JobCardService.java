package com.example.service;

import com.example.dto.JobCardDTO;
import com.example.entity.JobCard;
import com.example.entity.User;
import com.example.repository.JobCardRepository;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobCardService {

    @Autowired
    private JobCardRepository jobCardRepository;

    @Autowired
    private UserRepository userRepository;

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
//
//    public JobCard updateJobCard(String jobid, JobCardDTO jobCardDTO) throws Exception {
//        Optional<JobCard> optionalJobCard = jobCardRepository.findByJobid(jobid);
//        if (optionalJobCard.isEmpty()) {
//            throw new Exception("Job card not found");
//        }
//
//        JobCard jobCard = optionalJobCard.get();
//
//        // Update job card details
//        jobCard.setTitle(jobCardDTO.getTitle());
//        jobCard.setDescription(jobCardDTO.getDescription());
//        jobCard.setHoursnumber(jobCardDTO.getHoursnumber());
//        jobCard.setGeneratorid(jobCardDTO.getGeneratorid());
//
//        // Update the assignTo field based on the provided email
//        if (jobCardDTO.getAssignTo() != null && !jobCardDTO.getAssignTo().isEmpty()) {
//            Optional<User> userOptional = userRepository.findByEmail(jobCardDTO.getAssignTo());
//            if (userOptional.isPresent()) {
//                jobCard.setAssignTo(userOptional.get());
//            } else {
//                throw new Exception("User not found with the given email");
//            }
//        } else {
//            jobCard.setAssignTo(null);  // If no assignTo email is provided
//        }
//
//        // Update and save job card
//        return jobCardRepository.save(jobCard);
//    }


    // Method to update job card
    public JobCard updateJobCard(String jobid, JobCardDTO jobCardDTO) {
        JobCard jobCard = jobCardRepository.findById(Long.parseLong(jobid))
                .orElseThrow(() -> new RuntimeException("Job card not found"));

        // Update job card fields
        jobCard.setJobid(jobCardDTO.getJobCardId());
        jobCard.setGeneratorid(jobCardDTO.getGeneratorid());
        jobCard.setWorkstatus(jobCardDTO.getWorkstatus());
        jobCard.setLocation(jobCardDTO.getLocation());
        // Update any other fields as needed

        return jobCardRepository.save(jobCard);
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
