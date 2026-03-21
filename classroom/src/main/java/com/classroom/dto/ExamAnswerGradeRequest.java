package com.classroom.dto;

import lombok.Data;

@Data
public class ExamAnswerGradeRequest {

    private Long answerId;

    private Integer score;

    private String feedback;

    private Boolean correct;
}

