package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class HomeworkVO implements Serializable {

    private Long id;

    private Long courseId;

    private Long teacherId;

    private String teacherName;

    private String title;

    private String content;

    private String chapter;

    private LocalDateTime deadline;

    private Integer totalPoints;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
