package com.classroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.classroom.common.Result;
import com.classroom.entity.Course;
import com.classroom.entity.CourseClass;
import com.classroom.entity.Homework;
import com.classroom.entity.User;
import com.classroom.repository.CourseClassMapper;
import com.classroom.repository.CourseMapper;
import com.classroom.repository.HomeworkMapper;
import com.classroom.vo.CourseVO;
import com.classroom.vo.HomeworkVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/student")
@Tag(name = "学生服务")
public class StudentController {

    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final HomeworkMapper homeworkMapper;

    public StudentController(CourseMapper courseMapper, CourseClassMapper courseClassMapper, HomeworkMapper homeworkMapper) {
        this.courseMapper = courseMapper;
        this.courseClassMapper = courseClassMapper;
        this.homeworkMapper = homeworkMapper;
    }

    @GetMapping("/courses")
    @Operation(summary = "获取学生课程列表")
    public Result<List<CourseVO>> getMyCourses(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getClassId() == null) {
            return Result.success(new ArrayList<>());
        }

        // 获取该班级关联的所有课程ID
        List<CourseClass> courseClasses = courseClassMapper.selectList(
                new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getClassId, user.getClassId())
        );

        if (courseClasses.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<Long> courseIds = courseClasses.stream()
                .map(CourseClass::getCourseId)
                .collect(Collectors.toList());

        // 获取课程列表
        List<Course> courses = courseMapper.selectBatchIds(courseIds);

        List<CourseVO> result = courses.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(result);
    }

    @GetMapping("/homeworks")
    @Operation(summary = "获取学生作业列表")
    public Result<Page<HomeworkVO>> getMyHomeworks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getClassId() == null) {
            return Result.success(new Page<>(page, size));
        }

        // 获取该班级关联的所有课程ID
        List<CourseClass> courseClasses = courseClassMapper.selectList(
                new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getClassId, user.getClassId())
        );

        if (courseClasses.isEmpty()) {
            return Result.success(new Page<>(page, size));
        }

        List<Long> courseIds = courseClasses.stream()
                .map(CourseClass::getCourseId)
                .collect(Collectors.toList());

        // 查询这些课程下的作业
        Page<Homework> homeworkPage = homeworkMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Homework>()
                        .in(Homework::getCourseId, courseIds)
                        .orderByDesc(Homework::getCreateTime));

        Page<HomeworkVO> result = new Page<>();
        BeanUtils.copyProperties(homeworkPage, result, "records");
        result.setRecords(homeworkPage.getRecords().stream()
                .map(this::convertToHomeworkVO)
                .collect(Collectors.toList()));

        return Result.success(result);
    }

    private CourseVO convertToVO(Course course) {
        CourseVO vo = new CourseVO();
        BeanUtils.copyProperties(course, vo);
        return vo;
    }

    private HomeworkVO convertToHomeworkVO(Homework homework) {
        HomeworkVO vo = new HomeworkVO();
        BeanUtils.copyProperties(homework, vo);
        return vo;
    }
}
