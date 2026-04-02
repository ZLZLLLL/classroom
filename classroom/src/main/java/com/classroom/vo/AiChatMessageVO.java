package com.classroom.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatMessageVO {

    private String id;

    private String sessionId;

    private String question;

    private String answer;

    private LocalDateTime createdAt;
}

