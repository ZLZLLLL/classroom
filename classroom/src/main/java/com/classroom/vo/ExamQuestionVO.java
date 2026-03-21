package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ExamQuestionVO implements Serializable {

    private Long id;

    private Long examId;

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

