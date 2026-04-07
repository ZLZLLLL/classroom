package com.classroom.service;

import com.classroom.dto.AiGradeSuggestionResponse;
import com.classroom.dto.HomeworkAiGradeSuggestion;
import com.classroom.vo.AiChatMessageVO;
import com.classroom.entity.Answer;
import com.classroom.entity.ExamAnswer;
import com.classroom.entity.ExamQuestion;
import com.classroom.entity.Homework;
import com.classroom.entity.HomeworkSubmit;
import com.classroom.entity.Question;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import com.classroom.entity.User;
import com.classroom.repository.UserMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

@Service
public class ClassroomAiService {

    private static final Logger log = LoggerFactory.getLogger(ClassroomAiService.class);

    private static final Pattern SCORE_PATTERN = Pattern.compile("(?im)^SCORE\\s*:\\s*(\\d+)");
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("(?im)^CONFIDENCE\\s*:\\s*(low|medium|high)");
    private static final int MAX_CONTEXT_ROUNDS = 8;
    private GradingAssistant gradingAssistant;
    private HomeworkGradingAssistant homeworkGradingAssistant;

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private UserMapper userMapper;

    @Value("${ai.demo-mode:false}")
    private boolean demoMode;

    @PostConstruct
    public void initAssistants() {
        if (demoMode || chatLanguageModel == null) {
            return;
        }
        gradingAssistant = AiServices.builder(GradingAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
        homeworkGradingAssistant = AiServices.builder(HomeworkGradingAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }

    /**
     * 智能问答 - 回答关于课程内容的问题
     */
    public String answerQuestion(String question, List<AiChatMessageVO> history) {
        if (demoMode || chatLanguageModel == null) {
            return getDemoAnswer(question);
        }

        String systemPrompt = """
                你是一位专业的AI助教，名为"课堂小助手"。你的职责是帮助学生解答问题、解释知识点、辅导作业。
                请用友好、专业、易懂的语言回答用户的问题。
                如果问题与课程内容无关，请礼貌地引导用户询问与学习相关的问题。
                """;

        String prompt = buildChatPrompt(systemPrompt, question, history);
        return chatLanguageModel.chat(prompt);
    }

    private String buildChatPrompt(String systemPrompt, String question, List<AiChatMessageVO> history) {
        StringBuilder builder = new StringBuilder(systemPrompt)
                .append("\n\n")
                .append("以下是同一会话的历史上下文（按时间从早到晚）。")
                .append("如果历史与当前问题冲突，以当前问题为准。")
                .append("\n");

        if (history == null || history.isEmpty()) {
            builder.append("\n[历史为空]\n");
        } else {
            int start = Math.max(0, history.size() - MAX_CONTEXT_ROUNDS);
            for (int i = start; i < history.size(); i++) {
                AiChatMessageVO item = history.get(i);
                builder.append("\n用户: ")
                        .append(item.getQuestion() == null ? "" : item.getQuestion())
                        .append("\n助教: ")
                        .append(item.getAnswer() == null ? "" : item.getAnswer())
                        .append("\n");
            }
        }

        builder.append("\n当前用户问题: ").append(question);
        return builder.toString();
    }

    /**
     * 知识点解析 - 解释某个知识点
     */
    public String explainTopic(String topic) {
        if (demoMode || chatLanguageModel == null) {
            return getDemoExplanation(topic);
        }

        String systemPrompt = """
                你是一位知识渊博的老师，擅长用通俗易懂的方式解释复杂的概念。
                请用简洁清晰的语言解释用户提出的知识点，结合实际例子帮助理解。
                """;

        return chatLanguageModel.chat(systemPrompt + "\n\n请解释这个知识点: " + topic);
    }

    /**
     * 作业辅导 - 帮助学生完成作业
     */
    public String helpWithHomework(String homeworkContent, String question) {
        if (demoMode || chatLanguageModel == null) {
            return getDemoHomeworkHelp(homeworkContent, question);
        }

        String systemPrompt = """
                你是一位耐心的作业辅导老师。帮助学生理解作业要求，引导学生找到解题思路，
                而不是直接给出答案。请一步步引导学生思考，培养学生的自主学习能力。
                """;

        String userMessage = "作业内容: " + homeworkContent + "\n学生问题: " + question;

        return chatLanguageModel.chat(systemPrompt + "\n\n" + userMessage);
    }

    /**
     * 学习建议 - 根据学生情况给出学习建议
     */
    public String giveLearningAdvice(Long userId) {
        if (demoMode || chatLanguageModel == null) {
            return getDemoAdvice(userId);
        }

        User user = userMapper.selectById(userId);

        String systemPrompt = """
                你是一位学习顾问，根据学生的学习情况给出个性化的学习建议。
                请结合学生的学习历史和表现，给出具体可行的建议。
                """;

        String userMessage = "学生信息: " + (user != null ? user.getRealName() : "未知") + " (ID: " + userId + ")";

        return chatLanguageModel.chat(systemPrompt + "\n\n" + userMessage);
    }

    /**
     * 简答题AI评分建议（教师辅助）
     */
    public AiGradeSuggestionResponse suggestSubjectiveGrade(Question question, Answer answer) {
        int maxPoints = question.getPoints() == null ? 0 : question.getPoints();
        if (demoMode || chatLanguageModel == null || gradingAssistant == null) {
            return getDemoGradeSuggestion(maxPoints);
        }

        String reference = (question.getCorrectAnswer() == null ? "" : question.getCorrectAnswer());
        if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
            reference = reference.isBlank()
                    ? question.getExplanation()
                    : reference + "\n" + question.getExplanation();
        }

        String userMessage = buildGradingPrompt(question.getContent(), reference, answer.getContent(), maxPoints);
        return invokeSubjectiveAssistantSafely(userMessage, maxPoints, "qa", answer.getId());
    }

    /**
     * 考试简答题AI评分建议（教师辅助）
     */
    @SuppressWarnings("unused")
    public AiGradeSuggestionResponse suggestExamSubjectiveGrade(ExamQuestion question, ExamAnswer answer) {
        int maxPoints = question.getPoints() == null ? 0 : question.getPoints();
        if (demoMode || chatLanguageModel == null || gradingAssistant == null) {
            return getDemoGradeSuggestion(maxPoints);
        }

        String reference = (question.getCorrectAnswer() == null ? "" : question.getCorrectAnswer());
        if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
            reference = reference.isBlank()
                    ? question.getExplanation()
                    : reference + "\n" + question.getExplanation();
        }

        String userMessage = buildGradingPrompt(question.getContent(), reference, answer.getContent(), maxPoints);
        return invokeSubjectiveAssistantSafely(userMessage, maxPoints, "exam", answer.getId());
    }

    /**
     * 作业文本AI评分建议（教师辅助）
     */
    public HomeworkAiGradeSuggestion suggestHomeworkGrade(Homework homework, HomeworkSubmit submit) {
        if (submit.getFilePath() != null && !submit.getFilePath().isBlank()) {
            return new HomeworkAiGradeSuggestion(
                    submit.getId(), false, null, null, null, null,
                    "HOMEWORK_ATTACHMENT_UNSUPPORTED", "包含附件/图片，暂不支持AI评分"
            );
        }
        if (submit.getContent() == null || submit.getContent().isBlank()) {
            return new HomeworkAiGradeSuggestion(
                    submit.getId(), false, null, null, null, null,
                    "HOMEWORK_EMPTY_TEXT", "无文本内容，暂不支持AI评分"
            );
        }

        int maxPoints = homework.getTotalPoints() == null ? 0 : homework.getTotalPoints();
        if (demoMode || chatLanguageModel == null || homeworkGradingAssistant == null) {
            int score = Math.max(0, Math.min(maxPoints, Math.max(1, maxPoints / 2)));
            return new HomeworkAiGradeSuggestion(
                    submit.getId(),
                    true,
                    score,
                    "【演示模式】请核对要点覆盖与表达清晰度。",
                    "要点覆盖一般",
                    "medium",
                    null
            );
        }

        String prompt = buildHomeworkGradingPrompt(
                homework.getTitle(),
                homework.getContent(),
                submit.getContent(),
                maxPoints
        );
        return invokeHomeworkAssistantSafely(prompt, submit.getId(), maxPoints);
    }

    private AiGradeSuggestionResponse invokeSubjectiveAssistantSafely(String prompt, int maxPoints, String scene, Long answerId) {
        try {
            String raw = gradingAssistant.grade(prompt);
            AiGradeSuggestionResponse suggestion = parseSubjectiveSuggestion(raw, maxPoints);
            if (suggestion != null) {
                return suggestion;
            }
            return new AiGradeSuggestionResponse(
                    0,
                    "AI未返回可解析评分建议，请手动评分。",
                    "格式不符合约定",
                    "low",
                    "AI_OUTPUT_UNPARSABLE",
                    "AI响应未匹配SCORE/CONFIDENCE/CRITERIA/FEEDBACK约定"
            );
        } catch (RuntimeException ex) {
            log.warn("AI评分调用失败，准备重试: scene={}, answerId={}, error={}", scene, answerId, ex.getMessage());
            return retrySubjectiveAssistant(prompt, maxPoints, scene, answerId, ex);
        }
    }

    private AiGradeSuggestionResponse retrySubjectiveAssistant(String prompt, int maxPoints, String scene, Long answerId, RuntimeException firstError) {
        String hardenedPrompt = prompt + "\n\n补充要求：严格按SCORE/CONFIDENCE/CRITERIA/FEEDBACK四个键输出，不要输出其他内容。";
        try {
            String raw = gradingAssistant.grade(hardenedPrompt);
            AiGradeSuggestionResponse suggestion = parseSubjectiveSuggestion(raw, maxPoints);
            if (suggestion != null) {
                return suggestion;
            }
        } catch (RuntimeException retryError) {
            log.error("AI评分重试失败: scene={}, answerId={}, firstError={}, retryError={}",
                    scene,
                    answerId,
                    firstError.getMessage(),
                    retryError.getMessage());
        }
        return new AiGradeSuggestionResponse(
                0,
                "AI评分建议解析失败，请手动评分。",
                "AI返回格式异常",
                "low",
                "AI_CALL_FAILED",
                "AI调用或解析连续失败"
        );
    }

    private HomeworkAiGradeSuggestion invokeHomeworkAssistantSafely(String prompt, Long submitId, int maxPoints) {
        try {
            String raw = homeworkGradingAssistant.grade(prompt);
            HomeworkAiGradeSuggestion suggestion = parseHomeworkSuggestion(raw, submitId, maxPoints);
            if (suggestion != null) {
                return suggestion;
            }
            return new HomeworkAiGradeSuggestion(
                    submitId, false, null, null, null, null,
                    "AI_OUTPUT_UNPARSABLE", "AI未返回可解析评分建议"
            );
        } catch (RuntimeException ex) {
            log.warn("作业AI评分调用失败，准备重试: submitId={}, error={}", submitId, ex.getMessage());
            return retryHomeworkAssistant(prompt, submitId, maxPoints, ex);
        }
    }

    private HomeworkAiGradeSuggestion retryHomeworkAssistant(String prompt, Long submitId, int maxPoints, RuntimeException firstError) {
        String hardenedPrompt = prompt + "\n\n补充要求：严格按SCORE/CONFIDENCE/CRITERIA/FEEDBACK四个键输出，不要输出其他内容。";
        try {
            String raw = homeworkGradingAssistant.grade(hardenedPrompt);
            HomeworkAiGradeSuggestion suggestion = parseHomeworkSuggestion(raw, submitId, maxPoints);
            if (suggestion != null) {
                return suggestion;
            }
        } catch (RuntimeException retryError) {
            log.error("作业AI评分重试失败: submitId={}, maxPoints={}, firstError={}, retryError={}",
                    submitId,
                    maxPoints,
                    firstError.getMessage(),
                    retryError.getMessage());
        }
        return new HomeworkAiGradeSuggestion(
                submitId, false, null, null, null, null,
                "AI_CALL_FAILED", "AI评分建议解析失败，请手动评分"
        );
    }

    private AiGradeSuggestionResponse parseSubjectiveSuggestion(String raw, int maxPoints) {
        String normalized = normalizeAssistantOutput(raw);
        Integer score = extractScore(normalized);
        if (score == null) {
            return null;
        }
        int safeScore = Math.max(0, Math.min(maxPoints, score));
        String confidence = normalizeConfidence(extractConfidence(normalized));
        String criteria = extractField(normalized, "CRITERIA", "要点信息不足");
        String feedback = extractField(normalized, "FEEDBACK", "请结合标准答案人工复核");
        return new AiGradeSuggestionResponse(safeScore, feedback, criteria, confidence);
    }

    private HomeworkAiGradeSuggestion parseHomeworkSuggestion(String raw, Long submitId, int maxPoints) {
        String normalized = normalizeAssistantOutput(raw);
        Integer score = extractScore(normalized);
        if (score == null) {
            return null;
        }
        int safeScore = Math.max(0, Math.min(maxPoints, score));
        String confidence = normalizeConfidence(extractConfidence(normalized));
        String criteria = extractField(normalized, "CRITERIA", "要点信息不足");
        String feedback = extractField(normalized, "FEEDBACK", "请结合标准答案人工复核");
        return new HomeworkAiGradeSuggestion(submitId, true, safeScore, feedback, criteria, confidence, null, null);
    }

    private String normalizeAssistantOutput(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            int firstBreak = trimmed.indexOf('\n');
            if (firstBreak >= 0 && firstBreak + 1 < trimmed.length()) {
                trimmed = trimmed.substring(firstBreak + 1, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private Integer extractScore(String output) {
        Matcher matcher = SCORE_PATTERN.matcher(output);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extractConfidence(String output) {
        Matcher matcher = CONFIDENCE_PATTERN.matcher(output);
        if (!matcher.find()) {
            return "medium";
        }
        return matcher.group(1);
    }

    private String extractField(String output, String key, String defaultValue) {
        String regex = "(?is)^" + key + "\\s*:\\s*(.*?)\\s*(?=^SCORE\\s*:|^CONFIDENCE\\s*:|^CRITERIA\\s*:|^FEEDBACK\\s*:|\\z)";
        Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(output);
        if (!matcher.find()) {
            return defaultValue;
        }
        String value = matcher.group(1);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return sanitizeAiText(value.trim(), defaultValue);
    }

    private String sanitizeAiText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String sanitized = value
                .replaceAll("(?is)<think>.*?</think>", "")
                .replaceAll("(?is)</?think>", "")
                .trim();
        return sanitized.isBlank() ? defaultValue : sanitized;
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

    private String buildGradingPrompt(String question, String reference, String studentAnswer, int maxPoints) {
        String refText = (reference == null || reference.isBlank()) ? "无" : reference;
        return """
                你是教师阅卷助手，只给出简洁的评分建议。
                评分范围：0..""" + maxPoints + """

                评分规则：
                1) 关注关键要点是否覆盖、逻辑是否清晰、表达是否准确。
                2) 允许部分得分，但不要超过满分。
                3) 反馈要简短、可操作，避免过长推理。

                题目：""" + question + """

                参考答案/解析：""" + refText + """

                学生答案：""" + studentAnswer + """

                请严格按以下4行输出，不要加Markdown，不要加额外文本：
                SCORE: 0..""" + maxPoints + """
                CONFIDENCE: low|medium|high
                CRITERIA: 关键要点/扣分点（<= 60字）
                FEEDBACK: 1-2句短评（<= 120字）
                """;
    }

    private String buildHomeworkGradingPrompt(String title, String content, String answer, int maxPoints) {
        String safeTitle = title == null ? "" : title;
        String safeContent = content == null ? "" : content;
        return """
                你是教师作业批改助手，只给出简洁的评分建议。
                评分范围：0..""" + maxPoints + """

                评分规则：
                1) 关注关键要点是否覆盖、逻辑是否清晰、表达是否准确。
                2) 允许部分得分，但不要超过满分。
                3) 反馈要简短、可操作，避免过长推理。

                作业标题：""" + safeTitle + """

                作业内容：""" + safeContent + """

                学生答案：""" + answer + """

                请严格按以下4行输出，不要加Markdown，不要加额外文本：
                SCORE: 0..""" + maxPoints + """
                CONFIDENCE: low|medium|high
                CRITERIA: 关键要点/扣分点（<= 60字）
                FEEDBACK: 1-2句短评（<= 120字）
                """;
    }

    private AiGradeSuggestionResponse getDemoGradeSuggestion(int maxPoints) {
        int score = Math.max(0, Math.min(maxPoints, Math.max(1, maxPoints / 2)));
        return new AiGradeSuggestionResponse(
                score,
                "【演示模式】建议先核对要点覆盖度，再确认语言表达是否清晰。",
                "要点覆盖一般，表述可改进",
                "medium"
        );
    }

    private interface GradingAssistant {
        @SystemMessage("""
                你是负责简答题评分的AI助教。
                你必须严格输出4行键值：SCORE/CONFIDENCE/CRITERIA/FEEDBACK。
                不要输出Markdown，不要解释，不要额外文本。
                """)
        String grade(@UserMessage String input);
    }

    private interface HomeworkGradingAssistant {
        @SystemMessage("""
                你是负责作业评分的AI助教。
                你必须严格输出4行键值：SCORE/CONFIDENCE/CRITERIA/FEEDBACK。
                不要输出Markdown，不要解释，不要额外文本。
                """)
        String grade(@UserMessage String input);
    }

    // ===== 演示模式响应 =====

    private String getDemoAnswer(String question) {
        return "【演示模式】\n\n" +
                "您好！我是课堂小助手。\n\n" +
                "您的问题是：「" + question + "」\n\n" +
                "在演示模式下，我无法访问真实的AI服务。请配置有效的MiniMax API Key来启用完整功能。\n\n" +
                "要配置API Key，请在环境变量中设置 MINIMAX_API_KEY，或在 application.yml 中配置。\n\n" +
                "示例配置：\n" +
                "langchain4j:\n" +
                "  open-ai:\n" +
                "    chat-model:\n" +
                "      api-key: your-api-key-here";
    }

    private String getDemoExplanation(String topic) {
        return "【演示模式】\n\n" +
                "您好！我是课堂小助手。\n\n" +
                "要讲解的知识点：「" + topic + "」\n\n" +
                "在演示模式下，我无法访问真实的AI服务。请配置有效的MiniMax API Key来启用完整功能。\n\n" +
                "💡 提示：您可以配置API Key后，让我为您详细解释这个知识点。";
    }

    private String getDemoHomeworkHelp(String homeworkContent, String question) {
        return "【演示模式】\n\n" +
                "您好！我是您的作业辅导老师。\n\n" +
                "作业内容：「" + homeworkContent + "」\n\n" +
                "您的问题：「" + question + "」\n\n" +
                "在演示模式下，我无法访问真实的AI服务。请配置有效的MiniMax API Key来启用完整功能。\n\n" +
                "💡 提示：配置API Key后，我可以帮助您：\n" +
                "1. 理解作业要求\n" +
                "2. 分析解题思路\n" +
                "3. 引导您找到答案";
    }

    private String getDemoAdvice(Long userId) {
        return "【演示模式】\n\n" +
                "您好！我是您的学习顾问。\n\n" +
                "学生ID：「" + userId + "」\n\n" +
                "在演示模式下，我无法访问真实的AI服务。请配置有效的MiniMax API Key来启用完整功能。\n\n" +
                "💡 提示：配置API Key后，我可以根据您的学习情况给出个性化的学习建议。";
    }
}
