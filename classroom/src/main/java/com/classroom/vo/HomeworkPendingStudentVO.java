package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class HomeworkPendingStudentVO implements Serializable {

    private Long userId;

    private String userName;

    private Long classId;

    private String className;
}

