package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_vote_record")
public class VoteRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long voteId;

    private Long courseId;

    private Long userId;

    private String optionKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

