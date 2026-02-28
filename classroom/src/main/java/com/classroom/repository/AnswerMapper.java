package com.classroom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.Answer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnswerMapper extends BaseMapper<Answer> {
}
