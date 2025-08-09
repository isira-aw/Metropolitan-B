package com.example.met;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MetApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetApplication.class, args);
    }
}