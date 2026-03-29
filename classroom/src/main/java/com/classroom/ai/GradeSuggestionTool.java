package com.classroom.ai;

import com.classroom.dto.AiGradeSuggestionResponse;
import dev.langchain4j.agent.tool.Tool;

public class GradeSuggestionTool {

    private final ThreadLocal<AiGradeSuggestionResponse> suggestionHolder = new ThreadLocal<>();

    @SuppressWarnings("unused")
    @Tool("Finalize the subjective grading suggestion")
    public String finalizeGrade(int maxPoints, int score, String feedback, String criteriaSummary, String confidence) {
        int safeMaxPoints = Math.max(0, maxPoints);
        int clamped = Math.max(0, Math.min(safeMaxPoints, score));
        String safeFeedback = feedback == null ? "" : feedback.trim();
        String safeCriteria = criteriaSummary == null ? "" : criteriaSummary.trim();
        String safeConfidence = normalizeConfidence(confidence);
        suggestionHolder.set(new AiGradeSuggestionResponse(clamped, safeFeedback, safeCriteria, safeConfidence));
        return "OK";
    }

    @SuppressWarnings("unused")
    public void reset() {
        suggestionHolder.remove();
    }

    @SuppressWarnings("unused")
    public AiGradeSuggestionResponse getAndClearSuggestion() {
        AiGradeSuggestionResponse suggestion = suggestionHolder.get();
        suggestionHolder.remove();
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
