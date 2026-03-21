package com.classroom.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamQuestionRequest {

    private Integer type;

    private String content;

    private List<Option> options;

    private String correctAnswer;

    private String explanation;

    private Integer points;

    private Integer sortOrder;

    @Data
    public static class Option {
        private String label;
        private String content;
    }
}

