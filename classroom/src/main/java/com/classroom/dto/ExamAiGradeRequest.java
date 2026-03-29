package com.classroom.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamAiGradeRequest {

    private Long answerId;

    private List<Long> answerIds;
}


