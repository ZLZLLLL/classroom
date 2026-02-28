package com.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionCreateRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "提问内容不能为空")
    private String content;

    @NotNull(message = "题目类型不能为空")
    private Integer type;

    private List<Option> options;

    private String correctAnswer;

    private String explanation;

    private Integer points;

    private Integer duration;

    private List<Long> classIds;

    @Data
    public static class Option {
        private String label;
        private String content;
    }
}
