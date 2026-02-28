package com.classroom.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("edu_file")
public class File implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long uploaderId;

    /**
     * 类型: 1-课件 2-作业 3-共享资料
     */
    private Integer type;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
