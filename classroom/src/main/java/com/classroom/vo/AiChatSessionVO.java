package com.classroom.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatSessionVO {

    private String sessionId;

    private String title;

    private String lastQuestion;

    private String lastAnswer;

    private LocalDateTime lastTime;

    private Integer messageCount;
}

