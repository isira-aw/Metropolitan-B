package com.example.dto;

import com.example.entity.User;
import lombok.Data;

@Data
public class JobCardDTO {
    private String jobid;
    private String generatorid;
    private String title;
    private String description;
    private Integer hoursnumber;
    private String assignTo;
    private String workstatus;

    public String getJobid() {
        return jobid;
    }
    public String getAssignTo() {
        return assignTo;
    }

    public String getGeneratorid() {
        return generatorid;
    }
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }

    public Integer getHoursnumber() {
        return hoursnumber;
    }
    public String getWorkstatus() {
        return workstatus;
    }

}
