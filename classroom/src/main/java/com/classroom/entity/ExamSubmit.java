package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_exam_submit")
public class ExamSubmit implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long userId;

    /**
     * 状态: 1-作答中 2-已提交 3-已批改
     */
    private Integer status;

    private LocalDateTime submitTime;

    /**
     * 是否自动提交
     */
    private Integer autoSubmit;

    private Integer totalScore;

    private Integer objectiveScore;

    private Integer subjectiveScore;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

