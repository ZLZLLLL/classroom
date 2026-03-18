package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.entity.Course;
import com.classroom.entity.User;
import com.classroom.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/lottery")
@RequiredArgsConstructor
@Tag(name = "随机点名")
public class LotteryController {

    private final CourseService courseService;

    @PostMapping("/course/{courseId}/draw")
    @Operation(summary = "随机点名")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public Result<List<User>> draw(@PathVariable Long courseId,
                                  @RequestBody DrawRequest request,
                                  Authentication authentication) {
        User teacher = (User) authentication.getPrincipal();

        Course course = courseService.getById(courseId);
        if (course == null) {
            return Result.notFound("课程不存在");
        }
        if (!course.getTeacherId().equals(teacher.getId())) {
            return Result.forbidden("无权限操作此课程");
        }

        int count = request.getCount();

        List<User> students = courseService.getCourseStudents(courseId);
        if (students.isEmpty()) {
            return Result.badRequest("课程暂无学生");
        }

        if (count >= students.size()) {
            return Result.success(students);
        }

        // 不重复抽取：打乱后截取前 count
        List<User> copy = new ArrayList<>(students);
        Collections.shuffle(copy, new Random());
        return Result.success(copy.subList(0, count));
    }

    @Data
    public static class DrawRequest {
        @NotNull
        @Min(1)
        @Max(200)
        private Integer count;
    }
}
