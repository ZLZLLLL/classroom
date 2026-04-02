package com.classroom.service;

import com.classroom.entity.mongo.AiChatMessageDocument;
import com.classroom.exception.BusinessException;
import com.classroom.repository.mongo.AiChatMessageRepository;
import com.classroom.vo.AiChatMessageVO;
import com.classroom.vo.AiChatSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiChatHistoryService {

    private final AiChatMessageRepository aiChatMessageRepository;

    public void saveConversation(Long userId, String sessionId, String question, String answer) {
        AiChatMessageDocument message = new AiChatMessageDocument();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setQuestion(question);
        message.setAnswer(answer);
        message.setCreatedAt(LocalDateTime.now());
        aiChatMessageRepository.save(message);
    }

    public List<AiChatSessionVO> listSessions(Long userId) {
        List<AiChatMessageDocument> messages = aiChatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, AiChatSessionVO> sessionMap = new LinkedHashMap<>();

        for (AiChatMessageDocument message : messages) {
            if (message.getSessionId() == null || message.getSessionId().isBlank()) {
                continue;
            }
            AiChatSessionVO existing = sessionMap.get(message.getSessionId());
            if (existing == null) {
                AiChatSessionVO session = new AiChatSessionVO();
                session.setSessionId(message.getSessionId());
                session.setLastQuestion(message.getQuestion());
                session.setLastAnswer(message.getAnswer());
                session.setLastTime(message.getCreatedAt());
                session.setTitle(buildTitle(message.getQuestion()));
                session.setMessageCount(1);
                sessionMap.put(message.getSessionId(), session);
            } else {
                existing.setMessageCount(existing.getMessageCount() + 1);
            }
        }

        return new ArrayList<>(sessionMap.values());
    }

    public List<AiChatMessageVO> listSessionMessages(Long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException("sessionId不能为空");
        }

        List<AiChatMessageDocument> messages =
                aiChatMessageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId);

        return messages.stream().map(this::toMessageVO).toList();
    }

    private AiChatMessageVO toMessageVO(AiChatMessageDocument message) {
        AiChatMessageVO vo = new AiChatMessageVO();
        vo.setId(message.getId());
        vo.setSessionId(message.getSessionId());
        vo.setQuestion(message.getQuestion());
        vo.setAnswer(message.getAnswer());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    private String buildTitle(String question) {
        if (question == null || question.isBlank()) {
            return "新会话";
        }
        String trimmed = question.trim();
        return trimmed.length() <= 24 ? trimmed : trimmed.substring(0, 24) + "...";
    }
}

