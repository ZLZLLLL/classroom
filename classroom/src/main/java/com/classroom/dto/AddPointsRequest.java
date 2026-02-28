package com.classroom.dto;

import lombok.Data;

import java.util.List;

@Data
public class AddPointsRequest {
    private Long courseId;
    private List<Long> userIds;
    private Integer points;
    private String description;
}
