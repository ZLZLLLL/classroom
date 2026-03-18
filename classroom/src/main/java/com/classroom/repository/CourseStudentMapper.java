package com.classroom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.CourseStudent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseStudentMapper extends BaseMapper<CourseStudent> {
}


