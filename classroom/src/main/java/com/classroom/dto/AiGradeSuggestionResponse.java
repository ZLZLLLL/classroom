package com.classroom.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AiGradeSuggestionResponse {

    private Integer suggestedScore;

    private String feedback;

    private String criteriaSummary;

    private String confidence;

    /**
     * 机器可识别的失败原因码，成功时可为null。
     */
    private String reasonCode;

    /**
     * 人类可读的失败原因，成功时可为null。
     */
    private String reason;

    public AiGradeSuggestionResponse(Integer suggestedScore, String feedback, String criteriaSummary, String confidence) {
        this(suggestedScore, feedback, criteriaSummary, confidence, null, null);
    }

    public AiGradeSuggestionResponse(Integer suggestedScore,
                                     String feedback,
                                     String criteriaSummary,
                                     String confidence,
                                     String reasonCode,
                                     String reason) {
        this.suggestedScore = suggestedScore;
        this.feedback = feedback;
        this.criteriaSummary = criteriaSummary;
        this.confidence = confidence;
        this.reasonCode = reasonCode;
        this.reason = reason;
    }
}
