package com.example.met.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.example.met.enums.JobStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobCardTimeDetails {

    private UUID miniJobCardId;
    private UUID jobCardId;
    private String jobCardTitle;
    private JobStatus currentStatus;
    private LocalDate date;
    private String location;

    // Time spent in each status
    private String timeSpentOnHold;      // Format: "HH:mm"
    private String timeSpentInProgress;  // Format: "HH:mm"
    private String timeSpentAssigned;    // Format: "HH:mm"

    // Time in minutes for calculations
    private long onHoldMinutes;
    private long inProgressMinutes;
    private long assignedMinutes;
    private long totalMinutes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}