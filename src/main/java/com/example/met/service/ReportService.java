package com.example.met.service;

import com.example.met.entity.JobCard;
import com.example.met.entity.MiniJobCard;
import com.example.met.entity.Employee;
import com.example.met.repository.MiniJobCardRepository;
import com.example.met.repository.EmployeeRepository;
import com.example.met.dto.request.EmployeeTimeReportRequest;
import com.example.met.dto.response.EmployeeTimeReportResponse;
import com.example.met.dto.TimeSpentSummary;
import com.example.met.dto.JobCardTimeDetails;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportService {

    private static final int MAX_REPORT_DAYS = 14;
    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");

    private final MiniJobCardRepository miniJobCardRepository;
    private final EmployeeRepository employeeRepository;

    public ReportService(MiniJobCardRepository miniJobCardRepository,
                         EmployeeRepository employeeRepository) {
        this.miniJobCardRepository = miniJobCardRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public EmployeeTimeReportResponse generateEmployeeTimeReport(EmployeeTimeReportRequest request) {

        // Validate request
        validateReportRequest(request);

        // Verify employee exists
        Employee employee = employeeRepository.findByEmail(request.getEmployeeEmail())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with email: " + request.getEmployeeEmail()));

        // Fetch mini job cards for the date range
        List<MiniJobCard> miniJobCards = miniJobCardRepository.findByEmployeeEmailAndDateRange(
                request.getEmployeeEmail(),
                request.getStartDate(),
                request.getEndDate()
        );

        log.info("Found {} mini job cards for employee {} in date range {} to {}",
                miniJobCards.size(), request.getEmployeeEmail(), request.getStartDate(), request.getEndDate());

        // Convert to detailed time information
        List<JobCardTimeDetails> jobCardDetails = miniJobCards.stream()
                .map(this::convertToJobCardTimeDetails)
                .collect(Collectors.toList());

        // Calculate summary totals
        TimeSpentSummary summary = calculateTimeSpentSummary(jobCardDetails);

        // Build and return response
        return EmployeeTimeReportResponse.builder()
                .employeeEmail(employee.getEmail())
                .employeeName(employee.getName())
                .reportStartDate(request.getStartDate())
                .reportEndDate(request.getEndDate())
                .totalJobCards(jobCardDetails.size())
                .totalTimeSpent(summary)
                .jobCards(jobCardDetails)
                .generatedAt(LocalDateTime.now(SRI_LANKA_ZONE))
                .build();
    }

    private void validateReportRequest(EmployeeTimeReportRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        long daysBetween = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (daysBetween > MAX_REPORT_DAYS) {
            throw new IllegalArgumentException("Maximum report period is " + MAX_REPORT_DAYS + " days");
        }

        if (request.getEndDate().isAfter(LocalDate.now(SRI_LANKA_ZONE))) {
            throw new IllegalArgumentException("End date cannot be in the future");
        }
    }

    private JobCardTimeDetails convertToJobCardTimeDetails(MiniJobCard miniJobCard) {

        // Directly use long fields from MiniJobCard (minutes already stored)
        long onHoldMinutes = miniJobCard.getSpentOnOnHoldMinutes();
        long inProgressMinutes = miniJobCard.getSpentOnInProgressMinutes();
        long completedMinutes = miniJobCard.getSpentOnAssignedMinutes();
        long totalMinutes = onHoldMinutes + inProgressMinutes + completedMinutes;

        return JobCardTimeDetails.builder()
                .miniJobCardId(miniJobCard.getMiniJobCardId())
                .jobCardId(miniJobCard.getJobCard().getJobCardId())
                .jobCardTitle(getJobCardTitle(miniJobCard.getJobCard()))
                .currentStatus(miniJobCard.getStatus())
                .date(miniJobCard.getDate())
                .location(miniJobCard.getLocation())
                .timeSpentOnHold(minutesToTimeString(onHoldMinutes))
                .timeSpentInProgress(minutesToTimeString(inProgressMinutes))
                .timeSpentAssigned(minutesToTimeString(completedMinutes))
                .onHoldMinutes(onHoldMinutes)
                .inProgressMinutes(inProgressMinutes)
                .assignedMinutes(completedMinutes)
                .totalMinutes(totalMinutes)
                .createdAt(miniJobCard.getCreatedAt())
                .updatedAt(miniJobCard.getUpdatedAt())
                .build();
    }

    private String getJobCardTitle(JobCard jobCard) {
        String generatorName = jobCard.getGenerator().getName();
        String generatorKW = jobCard.getGenerator().getCapacity();
        return jobCard.getJobType().toString() + " - " + generatorName + "\n ( - " + generatorKW + "KW )";
    }

    private TimeSpentSummary calculateTimeSpentSummary(List<JobCardTimeDetails> jobCardDetails) {

        long totalOnHoldMinutes = jobCardDetails.stream()
                .mapToLong(JobCardTimeDetails::getOnHoldMinutes)
                .sum();

        long totalInProgressMinutes = jobCardDetails.stream()
                .mapToLong(JobCardTimeDetails::getInProgressMinutes)
                .sum();

        long totalAssignedMinutes = jobCardDetails.stream()
                .mapToLong(JobCardTimeDetails::getAssignedMinutes)
                .sum();

        long totalCombinedMinutes = totalOnHoldMinutes + totalInProgressMinutes + totalAssignedMinutes;

        return TimeSpentSummary.builder()
                .totalOnHoldTime(minutesToTimeString(totalOnHoldMinutes))
                .totalInProgressTime(minutesToTimeString(totalInProgressMinutes))
                .totalAssignedTime(minutesToTimeString(totalAssignedMinutes))
                .totalCombinedTime(minutesToTimeString(totalCombinedMinutes))
                .totalOnHoldMinutes(totalOnHoldMinutes)
                .totalInProgressMinutes(totalInProgressMinutes)
                .totalAssignedMinutes(totalAssignedMinutes)
                .totalCombinedMinutes(totalCombinedMinutes)
                .build();
    }

    private String minutesToTimeString(long totalMinutes) {
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
