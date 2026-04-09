package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("edu_attendance")
public class Attendance implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long userId;

    private LocalDateTime signTime;

    /**
     * 状态: 1-成功 2-迟到 3-缺勤
     */
    private Integer status;

    private Long operatorId;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
