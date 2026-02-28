package com.classroom.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SignInRequest {

    private Long courseId;

    private Long activityId;

    private BigDecimal latitude;

    private BigDecimal longitude;
}
