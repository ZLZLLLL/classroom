package com.classroom.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HomeworkAiGradeSuggestion {

    private Long submitId;

    private Boolean supported;

    private Integer suggestedScore;

    private String feedback;

    private String criteriaSummary;

    private String confidence;

    private String reasonCode;

    private String reason;

    public HomeworkAiGradeSuggestion(Long submitId,
                                     Boolean supported,
                                     Integer suggestedScore,
                                     String feedback,
                                     String criteriaSummary,
                                     String confidence,
                                     String reason) {
        this(submitId, supported, suggestedScore, feedback, criteriaSummary, confidence, null, reason);
    }

    public HomeworkAiGradeSuggestion(Long submitId,
                                     Boolean supported,
                                     Integer suggestedScore,
                                     String feedback,
                                     String criteriaSummary,
                                     String confidence,
                                     String reasonCode,
                                     String reason) {
        this.submitId = submitId;
        this.supported = supported;
        this.suggestedScore = suggestedScore;
        this.feedback = feedback;
        this.criteriaSummary = criteriaSummary;
        this.confidence = confidence;
        this.reasonCode = reasonCode;
        this.reason = reason;
    }
}
