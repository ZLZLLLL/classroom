package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ExamNoticeVO implements Serializable {

    private Long id;

    private Long examId;

    private Long courseId;

    private String courseName;

    private String title;

    private String content;

    private LocalDateTime createTime;
}

