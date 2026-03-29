package com.classroom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamAiGradeItemResponse {

    private Long answerId;

    private AiGradeSuggestionResponse suggestion;
}

