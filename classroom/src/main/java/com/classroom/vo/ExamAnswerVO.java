package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExamAnswerVO implements Serializable {

    private Long id;

    private Long submitId;

    private Long examId;

    private Long questionId;

    private String questionContent;

    private Integer questionType;

    private Integer questionPoints;

    private String content;

    private Integer isCorrect;

    private Integer score;

    private String feedback;
}

