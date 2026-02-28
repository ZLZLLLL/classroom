package com.classroom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.Homework;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HomeworkMapper extends BaseMapper<Homework> {
}
