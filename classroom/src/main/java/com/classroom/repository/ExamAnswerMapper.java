package com.classroom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.ExamAnswer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {
}

