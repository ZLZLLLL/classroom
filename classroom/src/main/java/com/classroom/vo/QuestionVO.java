package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class QuestionVO implements Serializable {

    private Long id;

    private Long courseId;

    private Long teacherId;

    private String content;

    private Integer type;

    private String options;

    private String correctAnswer;

    private String explanation;

    private Integer points;

    private Integer duration;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
