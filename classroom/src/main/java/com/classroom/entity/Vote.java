package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_vote")
public class Vote implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long teacherId;

    private String title;

    /**
     * 选项JSON [{"key":"A","content":"..."}]
     */
    private String options;

    /**
     * 目标班级ID列表JSON
     */
    private String classIds;

    /**
     * 状态: 1-进行中 2-已结束
     */
    private Integer status;

    /**
     * 类型: 1-单选 2-多选
     */
    private Integer type;

    /**
     * 是否匿名: 0-实名 1-匿名
     */
    private Integer anonymous;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}


