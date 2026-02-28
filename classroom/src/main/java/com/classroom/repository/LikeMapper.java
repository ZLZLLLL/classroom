package com.classroom.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.classroom.entity.Like;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {
}
