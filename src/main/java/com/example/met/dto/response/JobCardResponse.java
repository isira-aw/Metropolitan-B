package com.example.met.dto.response;

import com.example.met.enums.JobCardType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCardResponse {
    private UUID jobCardId;
    private UUID jobId;
    private GeneratorResponse generator;
    private JobCardType jobType;
    private LocalDate date;
    private LocalTime estimatedTime;
    private List<String> employeeEmails;
    private List<EmployeeResponse> assignedEmployees;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}