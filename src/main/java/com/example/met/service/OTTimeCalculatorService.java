package com.example.met.service;

import com.example.met.dto.request.OTTimeReportRequest;
import com.example.met.dto.response.OTTimeReportResponse;
import com.example.met.entity.Employee;
import com.example.met.entity.Log;
import com.example.met.entity.MiniJobCard;
import com.example.met.entity.OTtimeCalculator;
import com.example.met.repository.EmployeeRepository;
import com.example.met.repository.LogRepository;
import com.example.met.repository.OTTimeCalculatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTTimeCalculatorService {

    private final OTTimeCalculatorRepository otTimeCalculatorRepository;
    private final EmployeeService employeeService; // Add this dependency
    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");
    private final EmployeeRepository employeeRepository;
    private final LogRepository logRepository;

    @Transactional
    public void handleFirstLog(MiniJobCard miniJobCard) {
        try {
            Employee employee = miniJobCard.getEmployee();
            LocalDate today = LocalDate.now(SRI_LANKA_ZONE);
            LocalTime currentTime = LocalTime.now(SRI_LANKA_ZONE).withNano(0);

            // Check if there's already an entry for this employee today
            Optional<OTtimeCalculator> existingEntry = otTimeCalculatorRepository
                    .findByEmployeeAndDate(employee, today);

            if (existingEntry.isPresent()) {
                // Update existing entry - this is a subsequent log
                OTtimeCalculator entry = existingEntry.get();

                log.info("Updating existing OT entry for employee: {} on date: {}",
                        employee.getEmail(), today);

                // Update lasttime and lastlocation
                entry.updateLastTime(currentTime);
                entry.setLastLocation(miniJobCard.getLocation());

                // Calculate and update OT times
                calculateAndUpdateOT(entry);

                otTimeCalculatorRepository.save(entry);
                log.info("Updated OT entry for employee: {} - Last time: {}",
                        employee.getEmail(), currentTime);

            } else {
                // Create new entry - this is the first log of the day
                log.info("Creating new OT entry for employee: {} on date: {}",
                        employee.getEmail(), today);

                OTtimeCalculator newEntry = new OTtimeCalculator();
                newEntry.setEmployee(employee);
                newEntry.setDate(today);

                // CRITICAL: Use the helper method to set both times
                newEntry.setInitialTimes(currentTime);

                // Set locations
                newEntry.setFirstLocation(miniJobCard.getLocation());
                newEntry.setLastLocation(miniJobCard.getLocation());

                // Initialize OT times to zero (already done in entity defaults)
                newEntry.setMorningOTtime(LocalTime.of(0, 0, 0));
                newEntry.setEveningOTtime(LocalTime.of(0, 0, 0));

                otTimeCalculatorRepository.save(newEntry);
                log.info("Created new OT entry for employee: {} - First time: {}",
                        employee.getEmail(), currentTime);
            }

        } catch (Exception e) {
            log.error("Error handling first log for OT calculation for employee: {}",
                    miniJobCard.getEmployee().getEmail(), e);
            // Don't propagate the error as OT calculation shouldn't break main workflow
        }
    }

    @Transactional
    public void calculateAndUpdateOT(OTtimeCalculator entry) {
        try {
            if (entry.getFirsttime() == null || entry.getLasttime() == null) {
                log.warn("Cannot calculate OT - missing time data for employee: {} on date: {}",
                        entry.getEmployee().getEmail(), entry.getDate());
                return;
            }

            // Calculate morning OT (before 8:00 AM)
            LocalTime morningStart = LocalTime.of(8, 0); // 8:00 AM standard start
            LocalTime morningOT = calculateMorningOT(entry.getFirsttime(), morningStart);
            entry.setMorningOTtime(morningOT);

            // Calculate evening OT (after 5:00 PM)
            LocalTime eveningEnd = LocalTime.of(17, 0); // 5:00 PM standard end
            LocalTime eveningOT = calculateEveningOT(entry.getLasttime(), eveningEnd);
            entry.setEveningOTtime(eveningOT);

            log.info("Calculated OT for employee: {} on {}: Morning OT: {}, Evening OT: {}",
                    entry.getEmployee().getEmail(), entry.getDate(), morningOT, eveningOT);

        } catch (Exception e) {
            log.error("Error calculating OT for employee: {} on date: {}",
                    entry.getEmployee().getEmail(), entry.getDate(), e);
        }
    }

    private LocalTime calculateMorningOT(LocalTime firstTime, LocalTime standardStart) {
        if (firstTime.isBefore(standardStart)) {
            Duration duration = Duration.between(firstTime, standardStart);
            long minutes = duration.toMinutes();

            if (minutes > 0) {
                int hours = (int) Math.min(minutes / 60, 23); // Cap at 23 hours
                int mins = (int) Math.min(minutes % 60, 59);   // Cap at 59 minutes
                return LocalTime.of(hours, mins, 0);
            }
        }
        return LocalTime.of(0, 0, 0);
    }

    private LocalTime calculateEveningOT(LocalTime lastTime, LocalTime standardEnd) {
        if (lastTime.isAfter(standardEnd)) {
            Duration duration = Duration.between(standardEnd, lastTime);
            long minutes = duration.toMinutes();

            if (minutes > 0) {
                int hours = (int) Math.min(minutes / 60, 23); // Cap at 23 hours
                int mins = (int) Math.min(minutes % 60, 59);   // Cap at 59 minutes
                return LocalTime.of(hours, mins, 0);
            }
        }
        return LocalTime.of(0, 0, 0);
    }

    // Method to get or create OT entry for specific employee and date
    public Optional<OTtimeCalculator> getOTEntry(Employee employee, LocalDate date) {
        try {
            return otTimeCalculatorRepository.findByEmployeeAndDate(employee, date);
        } catch (Exception e) {
            log.error("Error fetching OT entry for employee: {} on date: {}",
                    employee.getEmail(), date, e);
            return Optional.empty();
        }
    }

    // Method to manually trigger OT calculation for a specific day
    @Transactional
    public void recalculateOTForDay(Employee employee, LocalDate date) {
        try {
            Optional<OTtimeCalculator> entryOpt = getOTEntry(employee, date);
            if (entryOpt.isPresent()) {
                OTtimeCalculator entry = entryOpt.get();
                calculateAndUpdateOT(entry);
                otTimeCalculatorRepository.save(entry);
                log.info("Recalculated OT for employee: {} on date: {}", employee.getEmail(), date);
            } else {
                log.warn("No OT entry found for employee: {} on date: {}", employee.getEmail(), date);
            }
        } catch (Exception e) {
            log.error("Error recalculating OT for employee: {} on date: {}", employee.getEmail(), date, e);
        }
    }
    // Add this method to your OTTimeCalculatorService class
    @Transactional
    public OTtimeCalculator handleEndSession(String employeeEmail, LocalDate date, LocalTime endTime, String endLocation) {
        try {
            log.info("Handling end session for employee: {} on date: {} at time: {}", employeeEmail, date, endTime);


            try {

                Employee employee = employeeRepository.findByEmail(employeeEmail)
                        .orElseThrow(() -> new IllegalArgumentException("Employee not found with email: " + employeeEmail));


                Log logEntry = new Log();
            logEntry.setEmployee(employee);
            logEntry.setAction("END_JOB_CARD");

            try {
                logEntry.setDate(LocalDate.now(SRI_LANKA_ZONE));
                logEntry.setTime(getSafeCurrentTime());
            } catch (DateTimeException e) {
                logEntry.setDate(LocalDate.now());
                logEntry.setTime(LocalTime.now().withNano(0));
            }

            logEntry.setGeneratorName(" ");
            logEntry.setStatus("END_DATE");
            logEntry.setLocation(endLocation);

            // Save the log entry
            logRepository.save(logEntry);
            log.info("Successfully created log entry for end the day by: {}", employeeEmail);

        } catch (Exception e) {
            log.error("Failed to create log entry for end: {}. Error: {}",
                    employeeEmail , e.getMessage(), e);
            // Don't propagate this error as it's not critical for the main operation
        }

            // Find employee
            Employee employee = employeeService.findByEmail(employeeEmail);

            // Find existing OT entry for the specified date
            Optional<OTtimeCalculator> existingEntryOpt = otTimeCalculatorRepository
                    .findByEmployeeAndDate(employee, date);

            if (existingEntryOpt.isPresent()) {
                OTtimeCalculator entry = existingEntryOpt.get();

                // Update last time and location
                entry.updateLastTime(endTime);
                entry.setLastLocation(endLocation);

                // Recalculate OT with the final end time
                calculateAndUpdateOT(entry);

                // Save the updated entry
                OTtimeCalculator savedEntry = otTimeCalculatorRepository.save(entry);

                log.info("Session ended for employee: {}. Final OT - Morning: {}, Evening: {}",
                        employeeEmail, savedEntry.getMorningOTtime(), savedEntry.getEveningOTtime());

                return savedEntry;
            } else {
                throw new IllegalArgumentException("No active session found for employee: " + employeeEmail + " on date: " + date);
            }

        } catch (Exception e) {
            log.error("Error handling end session for employee: {} on date: {}", employeeEmail, date, e);
            throw new RuntimeException("Failed to end session", e);
        }
    }

    private LocalTime getSafeCurrentTime() {
        try {
            return LocalTime.now(SRI_LANKA_ZONE).withNano(0); // Remove nanoseconds to prevent precision issues
        } catch (DateTimeException e) {
            log.warn("Error getting current time with timezone, using system default: {}", e.getMessage());
            return LocalTime.now().withNano(0); // System default without nanoseconds
        }
    }

    // Method for generating OT reports (used by ReportController)
    public OTTimeReportResponse generateOTTimeReport(OTTimeReportRequest request) {
        try {
            log.info("Generating OT report for employee: {} from {} to {}",
                    request.getEmployeeEmail(), request.getStartDate(), request.getEndDate());

            // Validate request
            validateOTReportRequest(request);

            // Find employee by email
            Employee employee = employeeService.findByEmail(request.getEmployeeEmail());

            // Get OT records for the date range
            List<OTtimeCalculator> otEntries = otTimeCalculatorRepository
                    .findByEmployeeEmailAndDateBetween(
                            request.getEmployeeEmail(),
                            request.getStartDate(),
                            request.getEndDate()
                    );

            // Build response
            OTTimeReportResponse response = new OTTimeReportResponse();
            response.setEmployeeEmail(request.getEmployeeEmail());
            response.setEmployeeName(employee.getName());
            response.setStartDate(request.getStartDate());
            response.setEndDate(request.getEndDate());

            // Convert entities to response records
            List<OTTimeReportResponse.OTRecord> otRecords = otEntries.stream()
                    .map(this::convertToOTRecord)
                    .toList();
            response.setOtRecords(otRecords);

            // Calculate totals
            calculateTotals(response, otEntries);

            log.info("Generated OT report for employee: {} with {} records",
                    request.getEmployeeEmail(), otRecords.size());

            return response;

        } catch (Exception e) {
            log.error("Error generating OT report for employee: {}", request.getEmployeeEmail(), e);
            throw new RuntimeException("Failed to generate OT report", e);
        }
    }

    // Helper methods for the report generation
    private void validateOTReportRequest(OTTimeReportRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (daysBetween > 31) {
            throw new IllegalArgumentException("Date range cannot exceed 31 days");
        }
    }

    private OTTimeReportResponse.OTRecord convertToOTRecord(OTtimeCalculator entry) {
        OTTimeReportResponse.OTRecord record = new OTTimeReportResponse.OTRecord();

        record.setDate(entry.getDate().toString());
        record.setFirstTime(formatTime(entry.getFirsttime()));
        record.setLastTime(formatTime(entry.getLasttime()));
        record.setFirstLocation(entry.getFirstLocation() != null ? entry.getFirstLocation() : "");
        record.setLastLocation(entry.getLastLocation() != null ? entry.getLastLocation() : "");
        record.setMorningOT(formatTime(entry.getMorningOTtime()));
        record.setEveningOT(formatTime(entry.getEveningOTtime()));

        // Calculate daily total OT
        LocalTime dailyTotal = entry.getTotalDailyOT();
        record.setDailyTotalOT(formatTime(dailyTotal));

        return record;
    }

    private void calculateTotals(OTTimeReportResponse response, List<OTtimeCalculator> entries) {
        int totalMorningMinutes = 0;
        int totalEveningMinutes = 0;

        for (OTtimeCalculator entry : entries) {
            if (entry.getMorningOTtime() != null) {
                totalMorningMinutes += entry.getMorningOTtime().getHour() * 60 + entry.getMorningOTtime().getMinute();
            }
            if (entry.getEveningOTtime() != null) {
                totalEveningMinutes += entry.getEveningOTtime().getHour() * 60 + entry.getEveningOTtime().getMinute();
            }
        }

        int totalMinutes = totalMorningMinutes + totalEveningMinutes;

        response.setTotalMorningOT(formatMinutesToTime(totalMorningMinutes));
        response.setTotalEveningOT(formatMinutesToTime(totalEveningMinutes));
        response.setTotalOT(formatMinutesToTime(totalMinutes));
    }

    private String formatTime(LocalTime time) {
        if (time == null) {
            return "00:00:00";
        }
        return time.toString();
    }

    private String formatMinutesToTime(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d:00", hours, minutes);
    }

    public Optional<OTtimeCalculator> getOTEntryByEmailAndDate(String employeeEmail, LocalDate date) {
        try {
            log.info("Getting OT entry for employee: {} on date: {}", employeeEmail, date);
            return otTimeCalculatorRepository.findByEmployeeEmailAndDate(employeeEmail, date);
        } catch (Exception e) {
            log.error("Error getting OT entry for employee: {} on date: {}", employeeEmail, date, e);
            return Optional.empty();
        }
    }
}