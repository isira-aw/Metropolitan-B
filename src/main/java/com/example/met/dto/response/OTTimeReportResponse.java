package com.example.met.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class OTTimeReportResponse {

        private String employeeEmail;
        private String employeeName;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        private List<OTRecord> otRecords;

        // OT Totals
        private String totalMorningOT;
        private String totalEveningOT;
        private String totalOT;

        // Status Time Totals
        private String totalOnHoldTime;
        private String totalAssignedTime;
        private String totalInProgressTime;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OTRecord {
            private String date;
            private String firstTime;
            private String lastTime;
            private String firstLocation;
            private String lastLocation;
            private String morningOT;
            private String eveningOT;
            private String dailyTotalOT;

            // Status time breakdown
            private String onHoldTime;
            private String assignedTime;
            private String inProgressTime;

            // Current status information
            private String currentStatus;
            private String lastStatus;

            // All locations visited during the day
            private List<String> allLocations;
            private String locationsSummary; // Comma-separated string of all locations
        }
    }