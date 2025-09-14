package com.example.met.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSpentSummary {

    private String totalOnHoldTime;      // Format: "HH:mm"
    private String totalInProgressTime;  // Format: "HH:mm"
    private String totalAssignedTime;    // Format: "HH:mm"
    private String totalCombinedTime;    // Format: "HH:mm"

    private long totalOnHoldMinutes;
    private long totalInProgressMinutes;
    private long totalAssignedMinutes;
    private long totalCombinedMinutes;

}