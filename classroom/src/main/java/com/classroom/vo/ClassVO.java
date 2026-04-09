package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClassVO implements Serializable {

    private Long id;

    private String name;

    private String grade;

    private String major;

    private String description;

    private Integer studentCount;
}
