package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class HomeworkSubmitStatusVO implements Serializable {

    private List<HomeworkSubmitVO> submitted;

    private List<HomeworkPendingStudentVO> notSubmitted;
}

