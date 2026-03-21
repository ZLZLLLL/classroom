package com.classroom.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.classroom.common.Result;
import com.classroom.dto.ExamCreateRequest;
import com.classroom.entity.Exam;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.service.CourseService;
import com.classroom.service.ExamService;
import com.classroom.vo.ExamDetailVO;
import com.classroom.vo.ExamVO;
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
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
@Tag(name = "考试管理")
public class ExamController {

    private final ExamService examService;
    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "创建考试")
    public Result<ExamVO> createExam(@Valid @RequestBody ExamCreateRequest request,
                                     Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Exam exam = examService.createExam(request, user.getId());
        return Result.success(examService.toExamVO(exam));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "发布考试")
    public Result<ExamVO> publishExam(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Exam exam = examService.publishExam(id, user.getId());
        return Result.success(examService.toExamVO(exam));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "获取教师考试列表")
    public Result<Page<ExamVO>> getTeacherExams(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<Exam> examPage = examService.getTeacherExams(user.getId(), page, size);

        Page<ExamVO> result = new Page<>();
        BeanUtils.copyProperties(examPage, result, "records");
        result.setRecords(examPage.getRecords().stream()
                .map(examService::toExamVO)
                .collect(Collectors.toList()));
        return Result.success(result);
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "获取学生考试列表")
    public Result<Page<ExamVO>> getStudentExams(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long courseId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<Exam> examPage = examService.getStudentExams(user.getId(), page, size, courseId);

        Page<ExamVO> result = new Page<>();
        BeanUtils.copyProperties(examPage, result, "records");
        result.setRecords(examPage.getRecords().stream()
                .map(examService::toExamVO)
                .collect(Collectors.toList()));
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取考试详情")
    public Result<ExamDetailVO> getExamDetail(@PathVariable Long id, Authentication authentication) {
        boolean includeAnswer = false;
        if (authentication != null) {
            User user = (User) authentication.getPrincipal();
            if (user.getRole() != null && user.getRole() == 1) {
                includeAnswer = true;
            } else if (user.getRole() != null && user.getRole() == 2) {
                List<Long> courseIds = courseService.getStudentCourseIds(user.getId());
                Exam exam = examService.getExamOrThrow(id);
                if (!courseIds.contains(exam.getCourseId())) {
                    throw new BusinessException("无权限查看该考试");
                }
            }
        }
        return Result.success(examService.getExamDetail(id, includeAnswer));
    }
}

