package com.classroom.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.classroom.common.Result;
import com.classroom.dto.CourseCreateRequest;
import com.classroom.dto.CourseUpdateRequest;
import com.classroom.entity.Class;
import com.classroom.entity.Course;
import com.classroom.entity.User;
import com.classroom.service.CourseService;
import com.classroom.vo.ClassVO;
import com.classroom.vo.CourseClassStudentsVO;
import com.classroom.vo.CourseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "课程管理")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "创建课程")
    public Result<CourseVO> createCourse(@Valid @RequestBody CourseCreateRequest request,
                                         Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Course course = courseService.createCourse(request, user.getId());
        return Result.success(convertToVO(course));
    }

    @GetMapping
    @Operation(summary = "获取课程列表")
    public Result<Page<CourseVO>> getCourseList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long teacherId = user.getRole() == 1 ? user.getId() : null;

        Page<Course> coursePage = courseService.getCourseList(page, size, keyword, teacherId);

        Page<CourseVO> result = new Page<>();
        BeanUtils.copyProperties(coursePage, result, "records");
        result.setRecords(coursePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取课程详情")
    public Result<CourseVO> getCourseById(@PathVariable Long id) {
        Course course = courseService.getCourseById(id);
        if (course == null) {
            return Result.notFound("课程不存在");
        }
        return Result.success(convertToVO(course));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "更新课程")
    public Result<CourseVO> updateCourse(@PathVariable Long id,
                                        @RequestBody CourseUpdateRequest request,
                                        Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long ownerId = isAdmin(user) ? null : user.getId();
        Course course = courseService.updateCourse(id, request, ownerId);
        return Result.success(convertToVO(course));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "删除课程")
    public Result<?> deleteCourse(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long ownerId = isAdmin(user) ? null : user.getId();
        courseService.deleteCourse(id, ownerId);
        return Result.success();
    }

    @PostMapping("/{id}/classes")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "添加班级到课程")
    public Result<?> addClassToCourse(@PathVariable Long id,
                                      @RequestBody Long classId,
                                      Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Course course = courseService.getById(id);
        if (course == null) {
            return Result.notFound("课程不存在");
        }
        if (!isAdmin(user) && !course.getTeacherId().equals(user.getId())) {
            return Result.forbidden("无权限操作此课程");
        }
        courseService.addClassToCourse(id, classId);
        return Result.success();
    }

    @DeleteMapping("/{courseId}/classes/{classId}")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "从课程移除班级")
    public Result<?> removeClassFromCourse(@PathVariable Long courseId,
                                           @PathVariable Long classId,
                                           Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Course course = courseService.getById(courseId);
        if (course == null) {
            return Result.notFound("课程不存在");
        }
        if (!isAdmin(user) && !course.getTeacherId().equals(user.getId())) {
            return Result.forbidden("无权限操作此课程");
        }
        courseService.removeClassFromCourse(courseId, classId);
        return Result.success();
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "获取课程学生列表")
    public Result<List<User>> getCourseStudents(@PathVariable Long id,
                                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Course course = courseService.getById(id);
        if (course == null) {
            return Result.notFound("课程不存在");
        }
        if (!isAdmin(user) && !course.getTeacherId().equals(user.getId())) {
            return Result.forbidden("无权限操作此课程");
        }
        List<User> students = courseService.getCourseStudents(id);
        return Result.success(students);
    }

    @GetMapping("/{id}/classes/students")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "获取课程按班级分组的学生列表")
    public Result<List<CourseClassStudentsVO>> getCourseClassStudents(@PathVariable Long id,
                                                                      Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Course course = courseService.getById(id);
        if (course == null) {
            return Result.notFound("课程不存在");
        }
        if (!isAdmin(user) && !course.getTeacherId().equals(user.getId())) {
            return Result.forbidden("无权限操作此课程");
        }
        return Result.success(courseService.getCourseClassStudents(id));
    }

    private CourseVO convertToVO(Course course) {
        CourseVO vo = new CourseVO();
        BeanUtils.copyProperties(course, vo);

        User teacher = courseService.getTeacherById(course.getTeacherId());
        if (teacher != null) {
            vo.setTeacherName(teacher.getRealName());
        }

        List<Class> classes = courseService.getCourseClasses(course.getId());
        if (!classes.isEmpty()) {
            vo.setClasses(classes.stream()
                    .map(this::convertClassToVO)
                    .collect(Collectors.toList()));
            vo.setClassIds(classes.stream()
                    .map(Class::getId)
                    .collect(Collectors.toList()));
            vo.setClassNames(classes.stream()
                    .map(Class::getName)
                    .collect(Collectors.toList()));
        }

        // 设置学生人数
        vo.setStudentCount(courseService.getCourseStudentCount(course.getId()));

        return vo;
    }

    private ClassVO convertClassToVO(Class aClass) {
        ClassVO vo = new ClassVO();
        BeanUtils.copyProperties(aClass, vo);
        return vo;
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() != null && user.getRole() == 3;
    }
}
