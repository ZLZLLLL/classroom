package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_question")
public class Question implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long teacherId;

    private String content;

    /**
     * 类型: 1-单选 2-多选 3-填空 4-简答
     */
    private Integer type;

    /**
     * 选项JSON [{"label":"A","content":"..."}]
     */
    private String options;

    private String correctAnswer;

    private String explanation;

    private Integer points;

    /**
     * 答题时间(秒)，0表示不限制
     */
    private Integer duration;

    /**
     * 状态: 1-进行中 2-已结束
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
