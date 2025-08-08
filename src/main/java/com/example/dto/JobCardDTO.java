package com.example.dto;

import com.example.entity.User;
import lombok.Data;

import java.time.LocalDate;

@Data
public class JobCardDTO {
    private String jobid;
    private String generatorid;
    private String title;
    private String description;
    private Integer hoursnumber;
    private String assignTo;
    private String workstatus;
    private LocalDate date;


}
