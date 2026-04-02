package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.entity.User;
import com.classroom.service.AiChatHistoryService;
import com.classroom.service.ClassroomAiService;
import com.classroom.vo.AiChatMessageVO;
import com.classroom.vo.AiChatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI管理")
public class AIController {

    private final ClassroomAiService classroomAiService;
    private final AiChatHistoryService aiChatHistoryService;

    @PostMapping("/chat")
    @Operation(summary = "AI智能问答")
    public Result<Map<String, String>> chat(@RequestBody Map<String, String> request,
                                            Authentication authentication) {
        String question = request.get("question");
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        User user = (User) authentication.getPrincipal();
        if (question == null || question.isBlank()) {
            return Result.error("问题不能为空");
        }
        //测试自动构建
        try {
            List<AiChatMessageVO> history = aiChatHistoryService.listSessionMessages(user.getId(), sessionId);
            String answer = classroomAiService.answerQuestion(question, history);
            aiChatHistoryService.saveConversation(user.getId(), sessionId, question, answer);
            return Result.success(Map.of(
                    "sessionId", sessionId,
                    "question", question,
                    "answer", answer
            ));
        } catch (Exception e) {
            return Result.error("AI服务调用失败: " + e.getMessage());
        }
    }

    @GetMapping("/sessions")
    @Operation(summary = "获取我的AI会话列表")
    public Result<List<AiChatSessionVO>> getSessions(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(aiChatHistoryService.listSessions(user.getId()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "获取我的AI会话消息列表")
    public Result<List<AiChatMessageVO>> getSessionMessages(@PathVariable String sessionId,
                                                            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(aiChatHistoryService.listSessionMessages(user.getId(), sessionId));
    }

    @PostMapping("/explain")
    @Operation(summary = "AI知识点解析")
    public Result<Map<String, String>> explain(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");

        try {
            String explanation = classroomAiService.explainTopic(topic);
            return Result.success(Map.of(
                    "topic", topic,
                    "explanation", explanation
            ));
        } catch (Exception e) {
            return Result.error("AI服务调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/homework-help")
    @Operation(summary = "AI作业辅导")
    public Result<Map<String, String>> homeworkHelp(@RequestBody Map<String, String> request) {
        String homeworkContent = request.get("homeworkContent");
        String question = request.getOrDefault("question", "请帮我理解这道题");

        try {
            String help = classroomAiService.helpWithHomework(homeworkContent, question);
            return Result.success(Map.of(
                    "homeworkContent", homeworkContent,
                    "question", question,
                    "help", help
            ));
        } catch (Exception e) {
            return Result.error("AI服务调用失败: " + e.getMessage());
        }
    }

    @GetMapping("/learning-advice")
    @Operation(summary = "AI学习建议")
    public Result<Map<String, String>> getLearningAdvice(Authentication authentication) {
        User user = (User) authentication.getPrincipal();

        try {
            String advice = classroomAiService.giveLearningAdvice(user.getId());
            return Result.success(Map.of(
                    "userId", String.valueOf(user.getId()),
                    "advice", advice
            ));
        } catch (Exception e) {
            return Result.error("AI服务调用失败: " + e.getMessage());
        }
    }
}
