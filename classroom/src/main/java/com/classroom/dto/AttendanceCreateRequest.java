package com.classroom.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AttendanceCreateRequest {

    private Long courseId;

    private Integer duration;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String location;

    private List<Long> classIds;
}
