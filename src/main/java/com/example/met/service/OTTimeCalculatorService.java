package com.example.met.service;

import com.example.met.dto.request.OTTimeReportRequest;
import com.example.met.dto.response.OTTimeReportResponse;
import com.example.met.entity.Employee;
import com.example.met.entity.Log;
import com.example.met.entity.MiniJobCard;
import com.example.met.entity.OTtimeCalculator;
import com.example.met.enums.JobStatus;
import com.example.met.exception.UnauthorizedException;
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
    private final EmployeeService employeeService;
    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");
    private final EmployeeRepository employeeRepository;
    private final LogRepository logRepository;

    @Transactional
    public void handleFirstLog(MiniJobCard miniJobCard) {
        try {
            Employee employee = miniJobCard.getEmployee();
            LocalDate today = LocalDate.now(SRI_LANKA_ZONE);
            LocalTime currentTime = LocalTime.now(SRI_LANKA_ZONE).withNano(0);
            LocalDateTime currentDateTime = getSafeCurrentDateTime();

            // Check if there's already an entry for this employee today
            Optional<OTtimeCalculator> existingEntry = otTimeCalculatorRepository
                    .findByEmployeeAndDate(employee, today);

            if (existingEntry.isPresent()) {
                // Update existing entry - this is a subsequent log
                OTtimeCalculator entry = existingEntry.get();

                log.info("Updating existing OT entry for employee: {} on date: {}",
                        employee.getEmail(), today);

                // Update status and calculate time spent in previous status
                String newStatus = miniJobCard.getStatus() != null ? miniJobCard.getStatus().toString() : "ASSIGNED";
                updateStatusAndCalculateTime(entry, newStatus, currentDateTime);

                // Update lasttime and lastlocation
                updateLastTime(entry, currentTime, currentDateTime);
                entry.setLastLocation(miniJobCard.getLocation());

                // Calculate and update OT times
                calculateAndUpdateOT(entry);

                otTimeCalculatorRepository.save(entry);
                log.info("Updated OT entry for employee: {} - Last time: {}, Status: {}",
                        employee.getEmail(), currentTime, newStatus);

            } else {
                // Create new entry - this is the first log of the day
                log.info("Creating new OT entry for employee: {} on date: {}",
                        employee.getEmail(), today);

                OTtimeCalculator newEntry = new OTtimeCalculator();
                newEntry.setEmployee(employee);
                newEntry.setDate(today);

                // Set both first and last time
                setInitialTimes(newEntry, currentTime, currentDateTime);

                // Set initial status
                String initialStatus = miniJobCard.getStatus() != null ? miniJobCard.getStatus().toString() : "ASSIGNED";
                newEntry.setCurrentstatus(initialStatus);
                newEntry.setStatusChangeTime(currentDateTime);

                // Set locations
                newEntry.setFirstLocation(miniJobCard.getLocation());
                newEntry.setLastLocation(miniJobCard.getLocation());

                // Initialize OT times and status times to zero
                initializeOTTimes(newEntry);

                otTimeCalculatorRepository.save(newEntry);
                log.info("Created new OT entry for employee: {} - First time: {}, Status: {}",
                        employee.getEmail(), currentTime, initialStatus);
            }

        } catch (Exception e) {
            log.error("Error handling first log for OT calculation for employee: {}",
                    miniJobCard.getEmployee().getEmail(), e);
            // Don't propagate the error as OT calculation shouldn't break main workflow
        }
    }

    @Transactional
    public void handleStatusChange(String employeeEmail, LocalDate date, String newStatus, LocalTime changeTime) {
        try {
            log.info("Handling status change for employee: {} to status: {} at time: {}",
                    employeeEmail, newStatus, changeTime);

            LocalDateTime currentDateTime = getSafeCurrentDateTime();

            Optional<OTtimeCalculator> entryOpt = otTimeCalculatorRepository
                    .findByEmployeeEmailAndDate(employeeEmail, date);

            if (entryOpt.isPresent()) {
                OTtimeCalculator entry = entryOpt.get();

                // Update status and calculate time spent in previous status
                updateStatusAndCalculateTime(entry, newStatus, currentDateTime);
                updateLastTime(entry, changeTime, currentDateTime);

                otTimeCalculatorRepository.save(entry);
                log.info("Updated status for employee: {} to {} at {}", employeeEmail, newStatus, changeTime);
            } else {
                log.warn("No OT entry found for employee: {} on date: {} for status change",
                        employeeEmail, date);
            }

        } catch (Exception e) {
            log.error("Error handling status change for employee: {} to status: {}",
                    employeeEmail, newStatus, e);
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

            // Calculate morning OT (before 8:30 AM)
            LocalTime morningStart = LocalTime.of(8, 30);
            LocalTime morningOT = calculateMorningOT(entry.getFirsttime(), morningStart);
            entry.setMorningOTtime(morningOT);

            // Calculate evening OT (after 5:00 PM)
            LocalTime eveningEnd = LocalTime.of(17, 0);
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
                int hours = (int) Math.min(minutes / 60, 23);
                int mins = (int) Math.min(minutes % 60, 59);
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
                int hours = (int) Math.min(minutes / 60, 23);
                int mins = (int) Math.min(minutes % 60, 59);
                return LocalTime.of(hours, mins, 0);
            }
        }
        return LocalTime.of(0, 0, 0);
    }

    @Transactional
    public OTtimeCalculator handleEndSession(String employeeEmail, LocalDate date, LocalTime endTime, String endLocation) {
        try {
            log.info("Handling end session for employee: {} on date: {} at time: {}", employeeEmail, date, endTime);

            LocalDateTime currentDateTime = getSafeCurrentDateTime();

            // Create log entry
            createEndSessionLog(employeeEmail, endLocation);

            // Find employee
            Employee employee = employeeService.findByEmail(employeeEmail);

            // Find existing OT entry for the specified date
            Optional<OTtimeCalculator> existingEntryOpt = otTimeCalculatorRepository
                    .findByEmployeeAndDate(employee, date);

            if (existingEntryOpt.isPresent()
                    && "END_JOB_CARD".equals(existingEntryOpt.get().getCurrentstatus())) {
                throw new UnauthorizedException("Current status is END_JOB_CARD, so it cannot be updated again.");
            }


            if (existingEntryOpt.isPresent()) {
                OTtimeCalculator entry = existingEntryOpt.get();

                // Handle COMPLETED to END_JOB_CARD transition if applicable
                if ("COMPLETED".equals(entry.getCurrentstatus())) {
                    handleCompletedToEndJobCard(entry, currentDateTime);
                }

                // Update final status if current status needs to be tracked
                if (entry.getCurrentstatus() != null) {
                    updateStatusAndCalculateTime(entry, "END_JOB_CARD", currentDateTime);
                }

                // Update last time and location
                updateLastTime(entry, endTime, currentDateTime);
                entry.setLastLocation(endLocation);

                // Finalize the day calculations
                finalizeDay(entry);

                // Recalculate OT with the final end time
                calculateAndUpdateOT(entry);

                // Save the updated entry
                OTtimeCalculator savedEntry = otTimeCalculatorRepository.save(entry);

                log.info("Session ended for employee: {}. Final OT - Morning: {}, Evening: {}, Status Times - OnHold: {}, Assigned: {}, InProgress: {}",
                        employeeEmail, savedEntry.getMorningOTtime(), savedEntry.getEveningOTtime(),
                        savedEntry.getSpentOnOnHold(), savedEntry.getSpentOnAssigned(), savedEntry.getSpentOnInProgress());

                return savedEntry;
            } else {
                throw new IllegalArgumentException("No active session found for employee: " + employeeEmail + " on date: " + date);
            }

        } catch (Exception e) {
            log.error("Error handling end session for employee: {} on date: {}", employeeEmail, date, e);
            throw new RuntimeException("Failed to end session", e);
        }
    }

    // Method for generating OT reports
    public OTTimeReportResponse generateOTTimeReport(OTTimeReportRequest request) {
        try {
            log.info("Generating OT report for employee: {} from {} to {}",
                    request.getEmployeeEmail(), request.getStartDate(), request.getEndDate());

            validateOTReportRequest(request);
            Employee employee = employeeService.findByEmail(request.getEmployeeEmail());

            List<OTtimeCalculator> otEntries = otTimeCalculatorRepository
                    .findByEmployeeEmailAndDateBetween(
                            request.getEmployeeEmail(),
                            request.getStartDate(),
                            request.getEndDate()
                    );

            OTTimeReportResponse response = new OTTimeReportResponse();
            response.setEmployeeEmail(request.getEmployeeEmail());
            response.setEmployeeName(employee.getName());
            response.setStartDate(request.getStartDate());
            response.setEndDate(request.getEndDate());

            List<OTTimeReportResponse.OTRecord> otRecords = otEntries.stream()
                    .map(this::convertToOTRecord)
                    .toList();
            response.setOtRecords(otRecords);

            calculateTotals(response, otEntries);

            log.info("Generated OT report for employee: {} with {} records",
                    request.getEmployeeEmail(), otRecords.size());

            return response;

        } catch (Exception e) {
            log.error("Error generating OT report for employee: {}", request.getEmployeeEmail(), e);
            throw new RuntimeException("Failed to generate OT report", e);
        }
    }

    // Helper methods for status time management
    private void updateStatusAndCalculateTime(OTtimeCalculator entry, String newStatus, LocalDateTime currentDateTime) {
        if (entry.getCurrentstatus() != null && entry.getStatusChangeTime() != null) {
            // Calculate time spent in previous status
            Duration timeSpent = Duration.between(entry.getStatusChangeTime(), currentDateTime);
            long minutesSpent = timeSpent.toMinutes();

            if (minutesSpent > 0) {
                addTimeToStatus(entry, entry.getCurrentstatus(), (int) minutesSpent);
            }
        }

        // Update to new status
        entry.setLaststatus(entry.getCurrentstatus());
        entry.setCurrentstatus(newStatus);
        entry.setStatusChangeTime(currentDateTime);
    }

    private void addTimeToStatus(OTtimeCalculator entry, String status, int minutesToAdd) {
        if (status == null || minutesToAdd <= 0) return;

        switch (status.toUpperCase()) {
            case "ON_HOLD":
                entry.setSpentOnOnHold(addMinutesToTime(entry.getSpentOnOnHold(), minutesToAdd));
                break;
            case "ASSIGNED":
                entry.setSpentOnAssigned(addMinutesToTime(entry.getSpentOnAssigned(), minutesToAdd));
                break;
            case "IN_PROGRESS":
                entry.setSpentOnInProgress(addMinutesToTime(entry.getSpentOnInProgress(), minutesToAdd));
                break;
        }
    }

    private LocalTime addMinutesToTime(LocalTime time, int minutesToAdd) {
        if (time == null) {
            time = LocalTime.of(0, 0, 0);
        }

        int totalMinutes = (time.getHour() * 60 + time.getMinute()) + minutesToAdd;
        int hours = (totalMinutes / 60) % 24; // Handle overflow beyond 24 hours
        int minutes = totalMinutes % 60;

        return LocalTime.of(hours, minutes, 0);
    }

    private void handleCompletedToEndJobCard(OTtimeCalculator entry, LocalDateTime endDateTime) {
        if (entry.getCurrentstatus() != null && entry.getCurrentstatus().equals("COMPLETED")
                && entry.getStatusChangeTime() != null) {

            Duration duration = Duration.between(entry.getStatusChangeTime(), endDateTime);
            long minutesSpent = duration.toMinutes();

            if (minutesSpent > 0) {
                // Add this time to ASSIGNED as per requirement
                entry.setSpentOnAssigned(addMinutesToTime(entry.getSpentOnAssigned(), (int) minutesSpent));
            }
        }
    }

    private void finalizeDay(OTtimeCalculator entry) {
        if (entry.getFirsttime() != null && entry.getLasttime() != null) {
            // Calculate total time between firsttime and lasttime
            Duration totalDuration = Duration.between(entry.getFirsttime(), entry.getLasttime());
            long totalMinutes = totalDuration.toMinutes();

            // Calculate time spent on IN_PROGRESS and ASSIGNED
            int inProgressMinutes = entry.getSpentOnInProgress().getHour() * 60 + entry.getSpentOnInProgress().getMinute();
            int assignedMinutes = entry.getSpentOnAssigned().getHour() * 60 + entry.getSpentOnAssigned().getMinute();

            // Calculate remaining time for ON_HOLD
            long onHoldMinutes = totalMinutes - inProgressMinutes - assignedMinutes;

            if (onHoldMinutes > 0) {
                // Add remaining time to ON_HOLD
                entry.setSpentOnOnHold(addMinutesToTime(entry.getSpentOnOnHold(), (int) onHoldMinutes));
            }
        }
    }

    private void setInitialTimes(OTtimeCalculator entry, LocalTime time, LocalDateTime currentDateTime) {
        if (time != null) {
            entry.setFirsttime(time);
            entry.setLasttime(time);
            entry.setLastTimeUpdateOTtime(currentDateTime);
        }
    }

    private void updateLastTime(OTtimeCalculator entry, LocalTime newTime, LocalDateTime currentDateTime) {
        if (newTime != null) {
            entry.setLasttime(newTime);
            entry.setLastTimeUpdateOTtime(currentDateTime);
        }
    }

    private void initializeOTTimes(OTtimeCalculator entry) {
        entry.setMorningOTtime(LocalTime.of(0, 0, 0));
        entry.setEveningOTtime(LocalTime.of(0, 0, 0));
        entry.setSpentOnOnHold(LocalTime.of(0, 0, 0));
        entry.setSpentOnAssigned(LocalTime.of(0, 0, 0));
        entry.setSpentOnInProgress(LocalTime.of(0, 0, 0));
    }

    private void createEndSessionLog(String employeeEmail, String endLocation) {
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

            logRepository.save(logEntry);
            log.info("Successfully created log entry for end the day by: {}", employeeEmail);

        } catch (Exception e) {
            log.error("Failed to create log entry for end: {}. Error: {}",
                    employeeEmail, e.getMessage(), e);
        }
    }

    private LocalTime getSafeCurrentTime() {
        try {
            return LocalTime.now(SRI_LANKA_ZONE).withNano(0);
        } catch (DateTimeException e) {
            log.warn("Error getting current time with timezone, using system default: {}", e.getMessage());
            return LocalTime.now().withNano(0);
        }
    }

    private LocalDateTime getSafeCurrentDateTime() {
        try {
            return LocalDateTime.now(SRI_LANKA_ZONE).withNano(0);
        } catch (DateTimeException e) {
            log.warn("Error getting current datetime with timezone, using system default: {}", e.getMessage());
            return LocalDateTime.now().withNano(0);
        }
    }

    // Helper methods for report generation
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

        // Add status time breakdown to record
        record.setOnHoldTime(formatTime(entry.getSpentOnOnHold()));
        record.setAssignedTime(formatTime(entry.getSpentOnAssigned()));
        record.setInProgressTime(formatTime(entry.getSpentOnInProgress()));
        record.setCurrentStatus(entry.getCurrentstatus());
        record.setLastStatus(entry.getLaststatus());

        // Calculate daily total OT
        LocalTime dailyTotal = entry.getTotalDailyOT();
        record.setDailyTotalOT(formatTime(dailyTotal));

        return record;
    }

    private void calculateTotals(OTTimeReportResponse response, List<OTtimeCalculator> entries) {
        int totalMorningMinutes = 0;
        int totalEveningMinutes = 0;
        int totalOnHoldMinutes = 0;
        int totalAssignedMinutes = 0;
        int totalInProgressMinutes = 0;

        for (OTtimeCalculator entry : entries) {
            if (entry.getMorningOTtime() != null) {
                totalMorningMinutes += entry.getMorningOTtime().getHour() * 60 + entry.getMorningOTtime().getMinute();
            }
            if (entry.getEveningOTtime() != null) {
                totalEveningMinutes += entry.getEveningOTtime().getHour() * 60 + entry.getEveningOTtime().getMinute();
            }
            if (entry.getSpentOnOnHold() != null) {
                totalOnHoldMinutes += entry.getSpentOnOnHold().getHour() * 60 + entry.getSpentOnOnHold().getMinute();
            }
            if (entry.getSpentOnAssigned() != null) {
                totalAssignedMinutes += entry.getSpentOnAssigned().getHour() * 60 + entry.getSpentOnAssigned().getMinute();
            }
            if (entry.getSpentOnInProgress() != null) {
                totalInProgressMinutes += entry.getSpentOnInProgress().getHour() * 60 + entry.getSpentOnInProgress().getMinute();
            }
        }

        int totalMinutes = totalMorningMinutes + totalEveningMinutes;

        response.setTotalMorningOT(formatMinutesToTime(totalMorningMinutes));
        response.setTotalEveningOT(formatMinutesToTime(totalEveningMinutes));
        response.setTotalOT(formatMinutesToTime(totalMinutes));
        response.setTotalOnHoldTime(formatMinutesToTime(totalOnHoldMinutes));
        response.setTotalAssignedTime(formatMinutesToTime(totalAssignedMinutes));
        response.setTotalInProgressTime(formatMinutesToTime(totalInProgressMinutes));
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

    // Existing utility methods
    public Optional<OTtimeCalculator> getOTEntryByEmailAndDate(String employeeEmail, LocalDate date) {
        try {
            log.info("Getting OT entry for employee: {} on date: {}", employeeEmail, date);
            return otTimeCalculatorRepository.findByEmployeeEmailAndDate(employeeEmail, date);
        } catch (Exception e) {
            log.error("Error getting OT entry for employee: {} on date: {}", employeeEmail, date, e);
            return Optional.empty();
        }
    }

    public Optional<OTtimeCalculator> getOTEntry(Employee employee, LocalDate date) {
        try {
            return otTimeCalculatorRepository.findByEmployeeAndDate(employee, date);
        } catch (Exception e) {
            log.error("Error fetching OT entry for employee: {} on date: {}",
                    employee.getEmail(), date, e);
            return Optional.empty();
        }
    }

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
}