package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AnswerVO implements Serializable {

    private Long id;

    private Long questionId;

    private Long userId;

    private String studentNo;

    private String realName;

    private String userName;

    private String content;

    private Integer isCorrect;

    private Integer score;

    private LocalDateTime createTime;
}
