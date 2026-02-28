package com.classroom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.CourseClass;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseClassMapper extends BaseMapper<CourseClass> {
}
