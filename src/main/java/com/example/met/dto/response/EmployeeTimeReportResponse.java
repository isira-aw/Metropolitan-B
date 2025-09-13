package com.example.met.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.example.met.dto.TimeSpentSummary;
import com.example.met.dto.JobCardTimeDetails;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeTimeReportResponse {

    private String employeeEmail;
    private String employeeName;
    private LocalDate reportStartDate;
    private LocalDate reportEndDate;
    private int totalJobCards;
    private TimeSpentSummary totalTimeSpent;
    private List<JobCardTimeDetails> jobCards;
    private LocalDateTime generatedAt;
}