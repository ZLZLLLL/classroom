package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CourseVO implements Serializable {

    private Long id;

    private String name;

    private String description;

    private Long teacherId;

    private String teacherName;

    private String coverUrl;

    private List<ClassVO> classes;

    private Integer studentCount;

    private List<Long> classIds;

    private List<String> classNames;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
