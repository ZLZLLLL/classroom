package com.classroom.service;

import com.classroom.dto.AiGradeSuggestionResponse;
import com.classroom.dto.HomeworkAiGradeSuggestion;
import com.classroom.entity.Answer;
import com.classroom.entity.Homework;
import com.classroom.entity.HomeworkSubmit;
import com.classroom.entity.Question;
import com.classroom.ai.GradeSuggestionTool;
import com.classroom.ai.HomeworkGradeSuggestionTool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import com.classroom.entity.User;
import com.classroom.repository.UserMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ClassroomAiService {

    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private UserMapper userMapper;

    @Value("${ai.demo-mode:false}")
    private boolean demoMode;

    /**
     * 智能问答 - 回答关于课程内容的问题
     */
    public String answerQuestion(String question) {
        if (demoMode || chatLanguageModel == null) {
            return getDemoAnswer(question);
        }

        String systemPrompt = """
                你是一位专业的AI助教，名为"课堂小助手"。你的职责是帮助学生解答问题、解释知识点、辅导作业。
                请用友好、专业、易懂的语言回答用户的问题。
                如果问题与课程内容无关，请礼貌地引导用户询问与学习相关的问题。
                """;

        return chatLanguageModel.chat(systemPrompt + "\n\n用户问题: " + question);
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
        if (demoMode || chatLanguageModel == null) {
            return getDemoGradeSuggestion(maxPoints);
        }

        GradeSuggestionTool tool = new GradeSuggestionTool(maxPoints);
        GradingAssistant assistant = AiServices.builder(GradingAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(tool)
                .build();

        String reference = (question.getCorrectAnswer() == null ? "" : question.getCorrectAnswer());
        if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
            reference = reference.isBlank()
                    ? question.getExplanation()
                    : reference + "\n" + question.getExplanation();
        }

        String userMessage = buildGradingPrompt(question.getContent(), reference, answer.getContent(), maxPoints);
        assistant.grade(userMessage);

        AiGradeSuggestionResponse suggestion = tool.getSuggestion();
        if (suggestion == null) {
            return new AiGradeSuggestionResponse(0, "AI未返回评分建议，请手动评分。", "未收到有效工具调用", "low");
        }
        return suggestion;
    }

    /**
     * 作业文本AI评分建议（教师辅助）
     */
    public HomeworkAiGradeSuggestion suggestHomeworkGrade(Homework homework, HomeworkSubmit submit) {
        if (submit.getFilePath() != null && !submit.getFilePath().isBlank()) {
            return new HomeworkAiGradeSuggestion(submit.getId(), false, null, null, null, null, "包含附件/图片，暂不支持AI评分");
        }
        if (submit.getContent() == null || submit.getContent().isBlank()) {
            return new HomeworkAiGradeSuggestion(submit.getId(), false, null, null, null, null, "无文本内容，暂不支持AI评分");
        }

        int maxPoints = homework.getTotalPoints() == null ? 0 : homework.getTotalPoints();
        if (demoMode || chatLanguageModel == null) {
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

        HomeworkGradeSuggestionTool tool = new HomeworkGradeSuggestionTool(maxPoints);
        HomeworkGradingAssistant assistant = AiServices.builder(HomeworkGradingAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(tool)
                .build();

        String prompt = buildHomeworkGradingPrompt(
                homework.getTitle(),
                homework.getContent(),
                submit.getContent(),
                maxPoints
        );
        assistant.grade(prompt);

        HomeworkAiGradeSuggestion suggestion = tool.getSuggestion();
        if (suggestion == null) {
            return new HomeworkAiGradeSuggestion(submit.getId(), false, null, null, null, null, "AI未返回评分建议");
        }
        suggestion.setSubmitId(submit.getId());
        return suggestion;
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

                请必须调用工具 finalizeGrade，给出：
                - score: 0..""" + maxPoints + """
                - feedback: 1-2 句短评（<= 120字）
                - criteriaSummary: 关键要点/扣分点（<= 60字）
                - confidence: low/medium/high
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

                请必须调用工具 finalizeHomeworkGrade，给出：
                - score: 0..""" + maxPoints + """
                - feedback: 1-2 句短评（<= 120字）
                - criteriaSummary: 关键要点/扣分点（<= 60字）
                - confidence: low/medium/high
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
                必须且只能调用一次工具 finalizeGrade；不要输出其他文本。
                """)
        String grade(@UserMessage String input);
    }

    private interface HomeworkGradingAssistant {
        @SystemMessage("""
                你是负责作业评分的AI助教。
                必须且只能调用一次工具 finalizeHomeworkGrade；不要输出其他文本。
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
