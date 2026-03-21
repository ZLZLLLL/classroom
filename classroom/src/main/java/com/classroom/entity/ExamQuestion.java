package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_exam_question")
public class ExamQuestion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    /**
     * 类型: 1-单选 2-多选 3-填空 4-简答
     */
    private Integer type;

    private String content;

    /**
     * 选项JSON [{"label":"A","content":"..."}]
     */
    private String options;

    private String correctAnswer;

    private String explanation;

    private Integer points;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

