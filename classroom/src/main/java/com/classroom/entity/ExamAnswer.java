package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_exam_answer")
public class ExamAnswer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submitId;

    private Long examId;

    private Long questionId;

    private Long userId;

    private String content;

    /**
     * 是否正确: 0-未评判 1-正确 2-错误
     */
    private Integer isCorrect;

    private Integer score;

    private String feedback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

