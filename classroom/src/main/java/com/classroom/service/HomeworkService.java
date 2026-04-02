package com.classroom.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.HomeworkCreateRequest;
import com.classroom.entity.Homework;
import com.classroom.exception.BusinessException;
import com.classroom.repository.HomeworkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class HomeworkService extends ServiceImpl<HomeworkMapper, Homework> {

    private final CourseService courseService;

    public Homework createHomework(HomeworkCreateRequest request, Long teacherId) {
        courseService.assertTeacherOwnsCourse(request.getCourseId(), teacherId);
        Homework homework = new Homework();
        BeanUtils.copyProperties(request, homework);
        homework.setTeacherId(teacherId);
        if (homework.getTotalPoints() == null) {
            homework.setTotalPoints(100);
        }
        // 序列化班级ID
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            homework.setClassIds(JSONUtil.toJsonStr(request.getClassIds()));
        }
        this.save(homework);
        return homework;
    }

    public Page<Homework> getCourseHomeworks(Long courseId, Integer page, Integer size) {
        return this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Homework>()
                        .eq(Homework::getCourseId, courseId)
                        .orderByDesc(Homework::getCreateTime));
    }

    public Page<Homework> getTeacherHomeworks(Long teacherId, Integer page, Integer size) {
        return this.page(new Page<>(page, size),
                new LambdaQueryWrapper<Homework>()
                        .eq(Homework::getTeacherId, teacherId)
                        .orderByDesc(Homework::getCreateTime));
    }

    public Homework getHomeworkById(Long id) {
        return this.getById(id);
    }

    public void updateHomework(Long id, HomeworkCreateRequest request, Long teacherId) {
        Homework homework = this.getById(id);
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        if (!homework.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限修改");
        }

        BeanUtils.copyProperties(request, homework);
        this.updateById(homework);
    }

    public void deleteHomework(Long id, Long teacherId) {
        Homework homework = this.getById(id);
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        if (!homework.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限删除");
        }
        this.removeById(id);
    }
}
