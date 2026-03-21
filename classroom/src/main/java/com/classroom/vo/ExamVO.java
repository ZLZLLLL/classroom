package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ExamVO implements Serializable {

    private Long id;

    private Long courseId;

    private String courseName;

    private Long teacherId;

    private String teacherName;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    private Integer totalPoints;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

