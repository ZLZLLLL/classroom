package com.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomeworkCreateRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "作业标题不能为空")
    private String title;

    private String content;

    private String chapter;

    private LocalDateTime deadline;

    private Integer totalPoints;

    private List<Long> classIds;
}
