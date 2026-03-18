package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class AdminDashboardVO implements Serializable {

    private Integer totalRegistered;

    private Integer teacherCount;

    private Integer studentCount;

    private String uptime;

    private String version;
}

