package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CourseClassStudentsVO implements Serializable {

    private Long classId;

    private String className;

    private Integer studentCount;

    private List<UserVO> students;
}

