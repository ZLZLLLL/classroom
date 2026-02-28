package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_attendance_activity")
public class AttendanceActivity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long teacherId;

    /**
     * 签到时长（分钟）
     */
    private Integer duration;

    /**
     * 签到位置
     */
    private String location;

    /**
     * 目标班级ID列表JSON
     */
    private String classIds;

    /**
     * 状态: 1-进行中 2-已结束
     */
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
