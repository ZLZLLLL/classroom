package com.classroom.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "ai_chat_messages")
public class AiChatMessageDocument {

    @Id
    private String id;

    private String sessionId;

    private Long userId;

    private String question;

    private String answer;

    private LocalDateTime createdAt;
}

