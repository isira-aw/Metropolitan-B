package com.example.controller;

import com.example.dto.CommonResponse;
import com.example.dto.JobCardDTO;
import com.example.entity.JobCard;
import com.example.service.JobCardService;
import jakarta.servlet.http.HttpServletRequest;
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


    // PUT endpoint to update Job Card
    @PutMapping("/update/{jobid}")
    public ResponseEntity<CommonResponse<JobCard>> updateJobCard(
            @PathVariable String jobid,
            @RequestBody JobCardDTO jobCardDTO,
            HttpServletRequest request) {
        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401)
                        .body(new CommonResponse<>("error", "No valid JWT token found", null));
            }

            String token = authHeader.substring(7);

            // Extract user information from JWT
            String email = jwtTokenProvider.getUsernameFromToken(token);
            String name = email; // In a real case, you might extract full name as well

            // Get client IP address for location
            String clientIp = getClientIpAddress(request);

            // Update job card
            JobCard updatedJobCard = jobCardService.updateJobCard(jobid, jobCardDTO);

            // Create event log
            jobEventLogService.createLog(
                    name,
                    email,
                    jobCardDTO.getGeneratorid(),
                    clientIp,
                    jobid,
                    "UPDATE"
            );

            return ResponseEntity.ok(new CommonResponse<>("success", "Job card updated successfully", updatedJobCard));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new CommonResponse<>("error", e.getMessage(), null));
        }
    }

    // Helper method to get client IP address
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0];
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
