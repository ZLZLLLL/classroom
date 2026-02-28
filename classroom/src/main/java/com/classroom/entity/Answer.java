package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_answer")
public class Answer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private Long userId;

    private String content;

    /**
     * 是否正确: 0-未评判 1-正确 2-错误
     */
    private Integer isCorrect;

    private Integer score;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
