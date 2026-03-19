package com.classroom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiGradeSuggestionResponse {

    private Integer suggestedScore;

    private String feedback;

    private String criteriaSummary;

    private String confidence;
}
