package com.classroom.service;

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
