package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_exam_notice")
public class ExamNotice implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long courseId;

    private String title;

    private String content;

    /**
     * 目标班级ID列表JSON
     */
    private String classIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

