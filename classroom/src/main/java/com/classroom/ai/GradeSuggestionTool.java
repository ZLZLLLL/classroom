package com.classroom.ai;

import com.classroom.dto.AiGradeSuggestionResponse;
import dev.langchain4j.agent.tool.Tool;

public class GradeSuggestionTool {

    private final int maxPoints;
    private AiGradeSuggestionResponse suggestion;

    public GradeSuggestionTool(int maxPoints) {
        this.maxPoints = Math.max(0, maxPoints);
    }

    @Tool("Finalize the subjective grading suggestion")
    public String finalizeGrade(int score, String feedback, String criteriaSummary, String confidence) {
        int clamped = Math.max(0, Math.min(maxPoints, score));
        String safeFeedback = feedback == null ? "" : feedback.trim();
        String safeCriteria = criteriaSummary == null ? "" : criteriaSummary.trim();
        String safeConfidence = normalizeConfidence(confidence);
        this.suggestion = new AiGradeSuggestionResponse(clamped, safeFeedback, safeCriteria, safeConfidence);
        return "OK";
    }

    public AiGradeSuggestionResponse getSuggestion() {
        return suggestion;
    }

    private String normalizeConfidence(String confidence) {
        if (confidence == null) {
            return "medium";
        }
        String normalized = confidence.trim().toLowerCase();
        if ("low".equals(normalized) || "medium".equals(normalized) || "high".equals(normalized)) {
            return normalized;
        }
        return "medium";
    }
}
