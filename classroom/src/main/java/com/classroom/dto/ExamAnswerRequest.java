package com.classroom.dto;

import lombok.Data;

@Data
public class ExamAnswerRequest {

    private Long questionId;

    private String content;
}

