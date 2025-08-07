package com.example.controller;

import com.example.dto.CommonResponse;
import com.example.dto.JobCardDTO;
import com.example.dto.LogJobCardDTO;
import com.example.entity.JobCard;
import com.example.entity.JobEventLog;
import com.example.service.JobCardService;
import com.example.service.JobEventLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @PutMapping("/update-empo/{jobid}")
    public ResponseEntity<CommonResponse<JobCard>> updateJobCardEmpoyer(@PathVariable String jobid, @RequestBody LogJobCardDTO logjobCardDTO) {
        try {
            JobCard updatedJobCard = jobCardService.updateJobCardEmpoyer(jobid, logjobCardDTO);
            return ResponseEntity.ok(new CommonResponse<>("success", "Job card updated successfully", updatedJobCard));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new CommonResponse<>("error", e.getMessage(), null));
        }
    }

    @PutMapping("/update-admin/{jobid}")
    public ResponseEntity<CommonResponse<JobCard>> updateJobCardAdmin(@PathVariable String jobid, @RequestBody JobCardDTO jobCardDTO) {
        try {
            JobCard updatedJobCard = jobCardService.updateJobCardAdmin(jobid, jobCardDTO);
            return ResponseEntity.ok(new CommonResponse<>("success", "Job card updated successfully", updatedJobCard));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new CommonResponse<>("error", e.getMessage(), null));
        }
    }

    @Autowired
    private JobEventLogService jobEventLogService;

    @RestController
    @RequestMapping("/api/job-event-logs")
    public class JobEventLogController {

        @Autowired
        private JobEventLogService jobEventLogService;

        // Get logs without authentication (no JWT required)
        @GetMapping("/get-logs")
        public ResponseEntity<CommonResponse<List<JobEventLog>>> getJobEventLogs(
                @RequestParam("startDate") String startDateStr,
                @RequestParam("endDate") String endDateStr) {

            try {
                // Convert the date strings to LocalDateTime
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime startDate = LocalDateTime.parse(startDateStr, formatter);
                LocalDateTime endDate = LocalDateTime.parse(endDateStr, formatter);

                // Fetch the logs from the service
                List<JobEventLog> jobEventLogs = jobEventLogService.getLogsByDateRange(startDate, endDate);

                // Return the successful response with logs
                return ResponseEntity.ok(new CommonResponse<>("success", "Logs fetched successfully", jobEventLogs));
            } catch (Exception e) {
                // Return error response in case of failure
                return ResponseEntity.status(500).body(new CommonResponse<>("error", "Failed to fetch logs: " + e.getMessage(), null));
            }
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
