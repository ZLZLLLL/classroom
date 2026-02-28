package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_homework_submit")
public class HomeworkSubmit implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long homeworkId;

    private Long userId;

    private String content;

    private String filePath;

    private LocalDateTime submitTime;

    private Integer score;

    private String feedback;

    /**
     * 状态: 1-待批改 2-已批改 3-逾期
     */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
