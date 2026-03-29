package com.classroom.ai;

import com.classroom.dto.HomeworkAiGradeSuggestion;
import dev.langchain4j.agent.tool.Tool;

public class HomeworkGradeSuggestionTool {

    private final ThreadLocal<HomeworkAiGradeSuggestion> suggestionHolder = new ThreadLocal<>();

    @SuppressWarnings("unused")
    @Tool("Finalize homework grading suggestion")
    public String finalizeHomeworkGrade(int maxPoints, int score, String feedback, String criteriaSummary, String confidence) {
        int safeMaxPoints = Math.max(0, maxPoints);
        int clamped = Math.max(0, Math.min(safeMaxPoints, score));
        String safeFeedback = feedback == null ? "" : feedback.trim();
        String safeCriteria = criteriaSummary == null ? "" : criteriaSummary.trim();
        String safeConfidence = normalizeConfidence(confidence);
        suggestionHolder.set(new HomeworkAiGradeSuggestion(null, true, clamped, safeFeedback, safeCriteria, safeConfidence, null));
        return "OK";
    }

    @SuppressWarnings("unused")
    public void reset() {
        suggestionHolder.remove();
    }

    @SuppressWarnings("unused")
    public HomeworkAiGradeSuggestion getAndClearSuggestion() {
        HomeworkAiGradeSuggestion suggestion = suggestionHolder.get();
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
