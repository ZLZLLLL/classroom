package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_course_student")
public class CourseStudent implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long userId;

    /**
     * 来源班级ID（用于追溯/移除班级关联时判断是否可移除）
     */
    private Long sourceClassId;

    /**
     * 状态：1-正常 0-已移除
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinTime;
}

