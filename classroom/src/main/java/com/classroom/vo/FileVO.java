package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FileVO implements Serializable {

    private Long id;

    private Long courseId;

    private Long uploaderId;

    private String uploaderName;

    private Integer type;

    private String fileName;

    private String filePath;

    private String fileUrl;

    private Long fileSize;

    private String fileType;

    private LocalDateTime createTime;
}
