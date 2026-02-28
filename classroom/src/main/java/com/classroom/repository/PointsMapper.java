package com.classroom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.Points;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PointsMapper extends BaseMapper<Points> {
}
