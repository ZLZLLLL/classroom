package com.classroom.dto;

import lombok.Data;

@Data
public class CourseUpdateRequest {

    private String name;

    private String description;

    private String coverUrl;
}
