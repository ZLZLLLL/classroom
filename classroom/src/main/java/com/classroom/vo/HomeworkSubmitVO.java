package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class HomeworkSubmitVO implements Serializable {

    private Long id;

    private Long homeworkId;

    private Long userId;

    private String studentNo;

    private String realName;

    private String userName;

    private String content;

    private String filePath;

    private LocalDateTime submitTime;

    private Integer score;

    private String feedback;

    private Integer status;

    private LocalDateTime createTime;
}
