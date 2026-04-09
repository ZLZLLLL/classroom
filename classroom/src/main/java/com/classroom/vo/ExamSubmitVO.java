package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamSubmitVO implements Serializable {

    private Long id;

    private Long examId;

    private Long userId;

    private String studentNo;

    private String realName;

    private String userName;

    private Integer status;

    private LocalDateTime submitTime;

    private Integer autoSubmit;

    private Integer totalScore;

    private Integer objectiveScore;

    private Integer subjectiveScore;

    private List<ExamAnswerVO> answers;
}

