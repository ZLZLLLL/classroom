package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ExamDetailVO implements Serializable {

    private ExamVO exam;

    private List<ExamQuestionVO> questions;
}

