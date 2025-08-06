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

    public void setGeneratorid(String generatorid) {
        this.generatorid = generatorid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getHoursnumber() {
        return hoursnumber;
    }

    public void setHoursnumber(Integer hoursnumber) {
        this.hoursnumber = hoursnumber;
    }

    public String getWorkstatus() {
        return workstatus;
    }

    public void setWorkstatus(String workstatus) {
        this.workstatus = workstatus;
    }
}
