package com.example.controller;

import com.example.dto.CommonResponse;
import com.example.dto.JobCardDTO;
import com.example.entity.JobCard;
import com.example.service.JobCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-cards")
public class JobCardController {

    @Autowired
    private JobCardService jobCardService;

    // Create Job Card
    @PostMapping("/create")
    public ResponseEntity<CommonResponse<JobCard>> createJobCard(@RequestBody JobCardDTO jobCardDTO) {
        try {
            // Create the Job Card
            JobCard createdJobCard = jobCardService.createJobCard(jobCardDTO);

            // Return response with the created job card data
            return ResponseEntity.ok(new CommonResponse<>("success", "Job card created successfully", createdJobCard));
        } catch (Exception e) {
            // Return error response if there's an issue
            return ResponseEntity.status(500).body(new CommonResponse<>("error", e.getMessage(), null));
        }
    }

    @PutMapping("/update/{jobid}")
    public ResponseEntity<CommonResponse<JobCard>> updateJobCard(@PathVariable String jobid, @RequestBody JobCardDTO jobCardDTO) {
        try {
            JobCard updatedJobCard = jobCardService.updateJobCard(jobid, jobCardDTO);
            return ResponseEntity.ok(new CommonResponse<>("success", "Job card updated successfully", updatedJobCard));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new CommonResponse<>("error", e.getMessage(), null));
        }
    }


    // Get Job Cards by Assignto (email)
    @GetMapping("/get-by-assign/{email}")
    public ResponseEntity<CommonResponse<List<JobCard>>> getJobCardsByAssignTo(@PathVariable String email) {
        try {
            List<JobCard> jobCards = jobCardService.getJobCardsByAssignTo(email);
            return ResponseEntity.ok(new CommonResponse<>("success", "Job cards fetched successfully", jobCards));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new CommonResponse<>("error", e.getMessage(), null));
        }
    }

    // Delete Job Card
    @DeleteMapping("/delete/{jobid}")
    public ResponseEntity<CommonResponse<String>> deleteJobCard(@PathVariable String jobid) {
        try {
            jobCardService.deleteJobCard(jobid);
            return ResponseEntity.ok(new CommonResponse<>("success", "Job card deleted successfully", jobid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new CommonResponse<>("error", e.getMessage(), null));
        }
    }
}
