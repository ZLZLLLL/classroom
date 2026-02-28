package com.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotBlank(message = "课程名称不能为空")
    private String name;

    private String description;

    private String coverUrl;

    private java.util.List<Long> classIds;
}
