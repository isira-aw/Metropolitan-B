package com.example.met.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class OTTimeReportResponse {

    private String employeeEmail;
    private String employeeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<OTRecord> otRecords;
    private String totalMorningOT;
    private String totalEveningOT;
    private String totalOT;

    @Data
    public static class OTRecord {
        private String date;
        private String firstTime;
        private String lastTime;
        private String firstLocation;
        private String lastLocation;
        private String morningOT;
        private String eveningOT;
        private String dailyTotalOT;
    }
}