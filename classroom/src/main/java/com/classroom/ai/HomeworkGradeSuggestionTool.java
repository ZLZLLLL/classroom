package com.classroom.ai;

import com.classroom.dto.HomeworkAiGradeSuggestion;
import dev.langchain4j.agent.tool.Tool;

public class HomeworkGradeSuggestionTool {

    private final int maxPoints;
    private HomeworkAiGradeSuggestion suggestion;

    public HomeworkGradeSuggestionTool(int maxPoints) {
        this.maxPoints = Math.max(0, maxPoints);
    }

    @Tool("Finalize homework grading suggestion")
    public String finalizeHomeworkGrade(int score, String feedback, String criteriaSummary, String confidence) {
        int clamped = Math.max(0, Math.min(maxPoints, score));
        String safeFeedback = feedback == null ? "" : feedback.trim();
        String safeCriteria = criteriaSummary == null ? "" : criteriaSummary.trim();
        String safeConfidence = normalizeConfidence(confidence);
        this.suggestion = new HomeworkAiGradeSuggestion(null, true, clamped, safeFeedback, safeCriteria, safeConfidence, null);
        return "OK";
    }

    public HomeworkAiGradeSuggestion getSuggestion() {
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
