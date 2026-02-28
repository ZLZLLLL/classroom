package com.classroom.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.entity.Class;
import com.classroom.repository.ClassMapper;
import org.springframework.stereotype.Service;

@Service
public class ClassService extends ServiceImpl<ClassMapper, Class> {
}
