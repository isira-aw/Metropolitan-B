package com.example.met.service;

import com.example.met.entity.JobCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import com.example.met.entity.MiniJobCard;
import com.example.met.entity.Employee;
import com.example.met.repository.MiniJobCardRepository;
import com.example.met.repository.EmployeeRepository;
import com.example.met.dto.request.EmployeeTimeReportRequest;
import com.example.met.dto.response.EmployeeTimeReportResponse;
import com.example.met.dto.TimeSpentSummary;
import com.example.met.dto.JobCardTimeDetails;

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

        // Convert LocalTime to minutes for calculations
        int onHoldMinutes = timeToMinutes(miniJobCard.getSpentOnOnHold());
        int inProgressMinutes = timeToMinutes(miniJobCard.getSpentOnInProgress());
        int assignedMinutes = timeToMinutes(miniJobCard.getSpentOnAssigned());
        int totalMinutes = onHoldMinutes + inProgressMinutes + assignedMinutes;

        return JobCardTimeDetails.builder()
                .miniJobCardId(miniJobCard.getMiniJobCardId())
                .jobCardId(miniJobCard.getJobCard().getJobCardId())
                .jobCardTitle(getJobCardTitle(miniJobCard.getJobCard()))
                .currentStatus(miniJobCard.getStatus())
                .date(miniJobCard.getDate())
                .location(miniJobCard.getLocation())
                .timeSpentOnHold(formatTime(miniJobCard.getSpentOnOnHold()))
                .timeSpentInProgress(formatTime(miniJobCard.getSpentOnInProgress()))
                .timeSpentAssigned(formatTime(miniJobCard.getSpentOnAssigned()))
                .onHoldMinutes(onHoldMinutes)
                .inProgressMinutes(inProgressMinutes)
                .assignedMinutes(assignedMinutes)
                .totalMinutes(totalMinutes)
                .createdAt(miniJobCard.getCreatedAt())
                .updatedAt(miniJobCard.getUpdatedAt())
                .build();
    }

    private String getJobCardTitle(JobCard jobCard) {
        String generatorName = jobCard.getGenerator().getName();
        String generatorKW = jobCard.getGenerator().getCapacity();
        return jobCard.getJobType().toString() + " - " + generatorName +"\n ( - "+generatorKW +"KW )";
    }

    private TimeSpentSummary calculateTimeSpentSummary(List<JobCardTimeDetails> jobCardDetails) {

        int totalOnHoldMinutes = jobCardDetails.stream()
                .mapToInt(JobCardTimeDetails::getOnHoldMinutes)
                .sum();

        int totalInProgressMinutes = jobCardDetails.stream()
                .mapToInt(JobCardTimeDetails::getInProgressMinutes)
                .sum();

        int totalAssignedMinutes = jobCardDetails.stream()
                .mapToInt(JobCardTimeDetails::getAssignedMinutes)
                .sum();

        int totalCombinedMinutes = totalOnHoldMinutes + totalInProgressMinutes + totalAssignedMinutes;

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

    private int timeToMinutes(LocalTime time) {
        if (time == null) {
            return 0;
        }
        return time.getHour() * 60 + time.getMinute();
    }

    private String formatTime(LocalTime time) {
        if (time == null) {
            return "00:00";
        }
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    private String minutesToTimeString(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }


}