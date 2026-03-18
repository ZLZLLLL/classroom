package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SystemAnnouncementVO implements Serializable {

    private Long id;

    private String title;

    private String content;

    private Long publisherId;

    private String publisherName;

    private LocalDateTime createTime;
}

