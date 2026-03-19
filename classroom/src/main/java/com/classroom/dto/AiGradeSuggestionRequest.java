package com.classroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiGradeSuggestionRequest {

    @NotNull
    private Long answerId;
}

