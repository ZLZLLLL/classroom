package com.classroom.dto;

import lombok.Data;

import java.util.List;

@Data
public class CourseUpdateRequest {

    private String name;

    private String description;

    private String coverUrl;

    /**
     * 关联班级ID列表（可多选）。不传则不变更。
     */
    private List<Long> classIds;
}
