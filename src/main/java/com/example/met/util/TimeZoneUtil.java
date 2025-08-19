package com.example.met.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class TimeZoneUtil {
    public static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static LocalDate getCurrentDate() {
        return LocalDate.now(SRI_LANKA_ZONE);
    }

    public static LocalTime getCurrentTime() {
        return LocalTime.now(SRI_LANKA_ZONE);
    }

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now(SRI_LANKA_ZONE);
    }

    public static ZonedDateTime getCurrentZonedDateTime() {
        return ZonedDateTime.now(SRI_LANKA_ZONE);
    }
}