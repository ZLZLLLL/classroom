package com.classroom.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class HomeworkAiGradeRequest {

    @NotEmpty
    private List<Long> submitIds;
}
