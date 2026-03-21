package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_exam")
public class Exam implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long teacherId;

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 考试时长(分钟)，0表示不限制
     */
    private Integer duration;

    private Integer totalPoints;

    /**
     * 状态: 1-未发布 2-进行中 3-已结束
     */
    private Integer status;

    /**
     * 目标班级ID列表JSON
     */
    private String classIds;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

