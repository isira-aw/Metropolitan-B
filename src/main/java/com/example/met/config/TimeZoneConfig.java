package com.example.met.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
@Slf4j
public class TimeZoneConfig {

    private static final String SRI_LANKA_TIMEZONE = "Asia/Colombo";

    @PostConstruct
    public void init() {
        TimeZone sriLankaTimeZone = TimeZone.getTimeZone(ZoneId.of(SRI_LANKA_TIMEZONE));
        TimeZone.setDefault(sriLankaTimeZone);
        System.setProperty("user.timezone", SRI_LANKA_TIMEZONE);

        // Log for debugging
        log.info("Default timezone set to: {}", TimeZone.getDefault().getID());
        log.info("Current time in Sri Lanka: {}", java.time.ZonedDateTime.now(ZoneId.of(SRI_LANKA_TIMEZONE)));

        // Verify PostgreSQL timezone handling
        log.info("Hibernate timezone property: {}", System.getProperty("hibernate.jdbc.time_zone"));
    }
}