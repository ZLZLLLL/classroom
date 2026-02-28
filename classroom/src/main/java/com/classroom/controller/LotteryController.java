package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.entity.User;
import com.classroom.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public Result<User> draw(@PathVariable Long courseId) {
        List<User> students = courseService.getCourseStudents(courseId);
        if (students.isEmpty()) {
            return Result.badRequest("课程暂无学生");
        }
        Random random = new Random();
        User selected = students.get(random.nextInt(students.size()));
        return Result.success(selected);
    }
}
