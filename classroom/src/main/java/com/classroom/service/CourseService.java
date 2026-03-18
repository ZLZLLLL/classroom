package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.CourseCreateRequest;
import com.classroom.dto.CourseUpdateRequest;
import com.classroom.entity.Class;
import com.classroom.entity.Course;
import com.classroom.entity.CourseClass;
import com.classroom.entity.CourseStudent;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.CourseClassMapper;
import com.classroom.repository.CourseStudentMapper;
import com.classroom.repository.CourseMapper;
import com.classroom.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService extends ServiceImpl<CourseMapper, Course> {

    private final CourseClassMapper courseClassMapper;
    private final UserMapper userMapper;
    private final ClassService classService;
    private final CourseStudentMapper courseStudentMapper;

    @Transactional
    public Course createCourse(CourseCreateRequest request, Long teacherId) {
        Course course = new Course();
        BeanUtils.copyProperties(request, course);
        course.setTeacherId(teacherId);

        this.save(course);

        // 关联班级
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            for (Long classId : request.getClassIds()) {
                CourseClass cc = new CourseClass();
                cc.setCourseId(course.getId());
                cc.setClassId(classId);
                courseClassMapper.insert(cc);

                // 同步班级学生到课程成员表（快照）
                syncStudentsFromClass(course.getId(), classId);
            }
        }

        return course;
    }

    @Transactional
    public Course updateCourse(Long id, CourseUpdateRequest request, Long teacherId) {
        Course course = this.getById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限修改此课程");
        }

        if (request.getName() != null) {
            course.setName(request.getName());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            course.setCoverUrl(request.getCoverUrl());
        }

        // 更新课程关联班级（如果传了 classIds，则重建关联并同步成员表）
        if (request.getClassIds() != null) {
            // 先移除所有班级关联
            courseClassMapper.delete(new LambdaQueryWrapper<CourseClass>()
                    .eq(CourseClass::getCourseId, id));

            // 标记本课程所有成员为移除，后续同步会按新班级集合重新置为正常
            courseStudentMapper.update(null, new LambdaUpdateWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, id)
                    .set(CourseStudent::getStatus, 0));

            if (!request.getClassIds().isEmpty()) {
                for (Long classId : request.getClassIds()) {
                    CourseClass cc = new CourseClass();
                    cc.setCourseId(id);
                    cc.setClassId(classId);
                    courseClassMapper.insert(cc);

                    syncStudentsFromClass(id, classId);
                }
            }
        }

        this.updateById(course);
        return course;
    }

    public void deleteCourse(Long id, Long teacherId) {
        Course course = this.getById(id);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限删除此课程");
        }

        this.removeById(id);
    }

    public Page<Course> getCourseList(Integer page, Integer size, String keyword, Long teacherId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, Course::getName, keyword)
                .eq(teacherId != null, Course::getTeacherId, teacherId)
                .orderByDesc(Course::getCreateTime);

        return this.page(new Page<>(page, size), wrapper);
    }

    public Course getCourseById(Long id) {
        return this.getById(id);
    }

    @Transactional
    public void addClassToCourse(Long courseId, Long classId) {
        Course course = this.getById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        // 检查是否已存在
        Long count = courseClassMapper.selectCount(new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getCourseId, courseId)
                .eq(CourseClass::getClassId, classId));

        if (count > 0) {
            throw new BusinessException("班级已存在");
        }

        CourseClass cc = new CourseClass();
        cc.setCourseId(courseId);
        cc.setClassId(classId);
        courseClassMapper.insert(cc);

        syncStudentsFromClass(courseId, classId);
    }

    @Transactional
    public void removeClassFromCourse(Long courseId, Long classId) {
        courseClassMapper.delete(new LambdaQueryWrapper<CourseClass>()
                .eq(CourseClass::getCourseId, courseId)
                .eq(CourseClass::getClassId, classId));

        // 将来源于该班级的成员标记为移除
        courseStudentMapper.update(null, new LambdaUpdateWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getSourceClassId, classId)
                .set(CourseStudent::getStatus, 0));
    }

    public List<Class> getCourseClasses(Long courseId) {
        List<CourseClass> courseClasses = courseClassMapper.selectList(
                new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getCourseId, courseId)
        );

        if (courseClasses.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> classIds = courseClasses.stream()
                .map(CourseClass::getClassId)
                .collect(Collectors.toList());

        return classService.listByIds(classIds);
    }

    public List<User> getCourseStudents(Long courseId) {
        List<CourseStudent> members = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStatus, 1));
        if (members.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = members.stream()
                .map(CourseStudent::getUserId)
                .distinct()
                .collect(Collectors.toList());

        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 2)
                .in(User::getId, userIds));
    }

    public User getTeacherById(Long teacherId) {
        return userMapper.selectById(teacherId);
    }

    public Integer getCourseStudentCount(Long courseId) {
        Long count = courseStudentMapper.selectCount(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStatus, 1));
        return count == null ? 0 : count.intValue();
    }

    private void syncStudentsFromClass(Long courseId, Long classId) {
        List<User> students = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 2)
                .eq(User::getClassId, classId));
        if (students.isEmpty()) {
            return;
        }

        for (User stu : students) {
            CourseStudent existing = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                    .eq(CourseStudent::getCourseId, courseId)
                    .eq(CourseStudent::getUserId, stu.getId()));

            if (existing == null) {
                CourseStudent cs = new CourseStudent();
                cs.setCourseId(courseId);
                cs.setUserId(stu.getId());
                cs.setSourceClassId(classId);
                cs.setStatus(1);
                courseStudentMapper.insert(cs);
            } else {
                existing.setStatus(1);
                existing.setSourceClassId(classId);
                courseStudentMapper.updateById(existing);
            }
        }
    }
}
