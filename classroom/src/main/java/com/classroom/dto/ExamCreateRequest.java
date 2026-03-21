package com.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamCreateRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "考试标题不能为空")
    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    private Integer totalPoints;

    private List<Long> classIds;

    @NotNull(message = "题目列表不能为空")
    private List<ExamQuestionRequest> questions;
}

