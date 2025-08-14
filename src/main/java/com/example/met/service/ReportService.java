package com.example.met.service;

import com.example.met.dto.request.ReportRequest;
import com.example.met.dto.response.ReportDataResponse;
import com.example.met.entity.Employee;
import com.example.met.entity.Log;
import com.example.met.repository.LogRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final LogRepository logRepository;
    private final EmployeeService employeeService;

    // Standard working hours
    private static final LocalTime WORK_START = LocalTime.of(8, 30);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final double STANDARD_WORK_HOURS = 8.5;


    public List<ReportDataResponse> generateReportData(ReportRequest request) {
        log.info("Generating report data for email: {} from {} to {}",
                request.getEmail(), request.getStartDate(), request.getEndDate());

        List<ReportDataResponse> reportData = new ArrayList<>();

        // Get all logs for the employee within date range
        List<Log> logs = logRepository.findByEmployeeEmailAndDateBetween(
                request.getEmail(),
                request.getStartDate(),
                request.getEndDate()
        );

        // Group logs by date
        Map<LocalDate, List<Log>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(Log::getDate));

        // Process each date
        for (LocalDate date = request.getStartDate();
             !date.isAfter(request.getEndDate());
             date = date.plusDays(1)) {

            List<Log> dayLogs = logsByDate.getOrDefault(date, new ArrayList<>());
            ReportDataResponse dayReport = processDayLogs(date, dayLogs);
            reportData.add(dayReport);
        }

        return reportData;
    }

    private ReportDataResponse processDayLogs(LocalDate date, List<Log> dayLogs) {
        ReportDataResponse report = new ReportDataResponse();
        report.setDate(date);

        if (dayLogs.isEmpty()) {
            // No activity for this day
            report.setGeneratorNames("No Activity");
            report.setFirstActionLocation("N/A");
            report.setLastActionLocation("N/A");
            report.setFullWorkingTime(0.0);
            report.setMorningOTTime(0.0);
            report.setEveningOTTime(0.0);
            report.setTotalOTTime(0.0);
            return report;
        }

        // Sort logs by time
        dayLogs.sort(Comparator.comparing(Log::getTime));

        // Get generator names (from related job cards through mini job cards)
        Set<String> generatorNames = new HashSet<>();
        for (Log log : dayLogs) {
            // You might need to adjust this based on your actual data structure
            // This assumes you can get generator info from the log context
            if (log.getLocation() != null) {
                generatorNames.add(extractGeneratorName(log));
            }
        }
        report.setGeneratorNames(String.join(", ", generatorNames));

        // First and last action locations
        Log firstLog = dayLogs.get(0);
        Log lastLog = dayLogs.get(dayLogs.size() - 1);

        report.setFirstActionLocation(firstLog.getLocation() != null ? firstLog.getLocation() : "N/A");
        report.setLastActionLocation(lastLog.getLocation() != null ? lastLog.getLocation() : "N/A");

        // Calculate working time
        LocalTime firstActionTime = firstLog.getTime();
        LocalTime lastActionTime = lastLog.getTime();

        double fullWorkingTime = calculateWorkingTime(firstActionTime, lastActionTime);
        report.setFullWorkingTime(fullWorkingTime);

        // Calculate OT times
        double morningOT = calculateMorningOT(firstActionTime);
        double eveningOT = calculateEveningOT(lastActionTime);

        report.setMorningOTTime(morningOT);
        report.setEveningOTTime(eveningOT);
        report.setTotalOTTime(morningOT + eveningOT);

        return report;
    }

    private String extractGeneratorName(Log log) {
        // This is a simplified approach - you might need to enhance this
        // based on your actual log structure and how generator info is stored
        String action = log.getAction();
        if (action != null && action.contains("GENERATOR")) {
            return "Generator Work";
        }
        return "General Work";
    }

    private double calculateWorkingTime(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return 0.0;
        }

        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        return Math.max(0, minutes / 60.0);
    }

    private double calculateMorningOT(LocalTime firstActionTime) {
        if (firstActionTime == null || !firstActionTime.isBefore(WORK_START)) {
            return 0.0;
        }

        long minutes = ChronoUnit.MINUTES.between(firstActionTime, WORK_START);
        return minutes / 60.0;
    }

    private double calculateEveningOT(LocalTime lastActionTime) {
        if (lastActionTime == null || !lastActionTime.isAfter(WORK_END)) {
            return 0.0;
        }

        long minutes = ChronoUnit.MINUTES.between(WORK_END, lastActionTime);
        return minutes / 60.0;
    }

    private byte[] createPDFDocument(Employee employee, ReportRequest request,
                                     List<ReportDataResponse> reportData) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate()); // Landscape for better table view
        PdfWriter.getInstance(document, baos);

        document.open();

        // Add title and header information
        addDocumentHeader(document, employee, request);

        // Add the data table
        addDataTable(document, reportData);

        // Add summary
        addSummary(document, reportData);

        document.close();

        return baos.toByteArray();
    }

    private void addDocumentHeader(Document document, Employee employee, ReportRequest request)
            throws DocumentException {

        // Title
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Employee Work Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Employee and date info
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        Paragraph employeeInfo = new Paragraph();
        employeeInfo.add(new Chunk("Employee: ", headerFont));
        employeeInfo.add(new Chunk(employee.getName() + " (" + employee.getEmail() + ")", normalFont));
        employeeInfo.setSpacingAfter(10);
        document.add(employeeInfo);

        Paragraph dateRange = new Paragraph();
        dateRange.add(new Chunk("Report Period: ", headerFont));
        dateRange.add(new Chunk(request.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " to " + request.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
        dateRange.setSpacingAfter(20);
        document.add(dateRange);
    }

    private void addDataTable(Document document, List<ReportDataResponse> reportData)
            throws DocumentException {

        // Create table with 7 columns
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        // Set column widths
        float[] columnWidths = {12f, 20f, 18f, 18f, 12f, 10f, 10f};
        table.setWidths(columnWidths);

        // Add headers
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        String[] headers = {"Date", "Generator Names", "First Action Location",
                "Last Action Location", "Full Working Time (hrs)", "Morning OT (hrs)", "Evening OT (hrs)"};

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new BaseColor(70, 130, 180));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Add data rows
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (ReportDataResponse data : reportData) {
            // Date
            table.addCell(createDataCell(data.getDate().format(dateFormatter), dataFont));

            // Generator Names
            table.addCell(createDataCell(data.getGeneratorNames(), dataFont));

            // First Action Location
            table.addCell(createDataCell(data.getFirstActionLocation(), dataFont));

            // Last Action Location
            table.addCell(createDataCell(data.getLastActionLocation(), dataFont));

            // Full Working Time
            table.addCell(createDataCell(String.format("%.2f", data.getFullWorkingTime()), dataFont));

            // Morning OT
            table.addCell(createDataCell(String.format("%.2f", data.getMorningOTTime()), dataFont));

            // Evening OT
            table.addCell(createDataCell(String.format("%.2f", data.getEveningOTTime()), dataFont));
        }

        document.add(table);
    }

    private PdfPCell createDataCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private void addSummary(Document document, List<ReportDataResponse> reportData)
            throws DocumentException {

        // Calculate totals
        double totalWorkingHours = reportData.stream()
                .mapToDouble(ReportDataResponse::getFullWorkingTime)
                .sum();

        double totalMorningOT = reportData.stream()
                .mapToDouble(ReportDataResponse::getMorningOTTime)
                .sum();

        double totalEveningOT = reportData.stream()
                .mapToDouble(ReportDataResponse::getEveningOTTime)
                .sum();

        double totalOT = totalMorningOT + totalEveningOT;

        // Add summary section
        Font summaryHeaderFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Font summaryFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

        Paragraph summaryTitle = new Paragraph("Summary", summaryHeaderFont);
        summaryTitle.setSpacingBefore(20);
        summaryTitle.setSpacingAfter(10);
        document.add(summaryTitle);

        Paragraph totalHours = new Paragraph();
        totalHours.add(new Chunk("Total Working Hours: ", summaryHeaderFont));
        totalHours.add(new Chunk(String.format("%.2f hrs", totalWorkingHours), summaryFont));
        document.add(totalHours);

        Paragraph totalOTHours = new Paragraph();
        totalOTHours.add(new Chunk("Total Overtime Hours: ", summaryHeaderFont));
        totalOTHours.add(new Chunk(String.format("%.2f hrs (Morning: %.2f hrs, Evening: %.2f hrs)",
                totalOT, totalMorningOT, totalEveningOT), summaryFont));
        document.add(totalOTHours);

        // Add generation timestamp
        Paragraph timestamp = new Paragraph();
        timestamp.add(new Chunk("Report Generated: ", summaryHeaderFont));
        timestamp.add(new Chunk(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), summaryFont));
        timestamp.setSpacingBefore(20);
        document.add(timestamp);
    }
}