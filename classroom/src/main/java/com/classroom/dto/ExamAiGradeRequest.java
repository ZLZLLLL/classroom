package com.classroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamAiGradeRequest {

    @NotNull
    private Long answerId;
}

