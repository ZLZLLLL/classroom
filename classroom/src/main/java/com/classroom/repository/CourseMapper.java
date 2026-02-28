package com.classroom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
