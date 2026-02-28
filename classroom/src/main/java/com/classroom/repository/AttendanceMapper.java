package com.classroom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.Attendance;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttendanceMapper extends BaseMapper<Attendance> {
}
