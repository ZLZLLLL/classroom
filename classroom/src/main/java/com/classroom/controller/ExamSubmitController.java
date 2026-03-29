package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.dto.ExamAiGradeRequest;
import com.classroom.dto.ExamAiGradeItemResponse;
import com.classroom.dto.ExamGradeRequest;
import com.classroom.dto.ExamSubmitRequest;
import com.classroom.entity.ExamSubmit;
import com.classroom.entity.User;
import com.classroom.service.ExamSubmitService;
import com.classroom.vo.ExamSubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam-submits")
@RequiredArgsConstructor
@Tag(name = "考试提交管理")
public class ExamSubmitController {

    private final ExamSubmitService examSubmitService;

    @PostMapping("/{examId}/start")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "开始考试")
    public Result<ExamSubmitVO> startExam(@PathVariable Long examId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ExamSubmit submit = examSubmitService.startExam(examId, user.getId());
        return Result.success(examSubmitService.toSubmitVO(submit, null, null));
    }

    @PutMapping("/{examId}/progress")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "保存考试进度")
    public Result<ExamSubmitVO> saveProgress(@PathVariable Long examId,
                                             @Valid @RequestBody ExamSubmitRequest request,
                                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ExamSubmit submit = examSubmitService.saveProgress(examId, request, user.getId());
        return Result.success(examSubmitService.toSubmitVO(submit, null, null));
    }

    @PostMapping("/{examId}/submit")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "提交考试")
    public Result<ExamSubmitVO> submitExam(@PathVariable Long examId,
                                           @Valid @RequestBody ExamSubmitRequest request,
                                           Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ExamSubmit submit = examSubmitService.submitExam(examId, request, user.getId(), false);
        return Result.success(examSubmitService.toSubmitVO(submit, null, null));
    }

    @GetMapping("/{examId}/my")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "获取我的考试提交")
    public Result<ExamSubmitVO> getMySubmit(@PathVariable Long examId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(examSubmitService.getMySubmitDetail(examId, user.getId()));
    }

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "获取考试提交列表(教师)")
    public Result<List<ExamSubmitVO>> getExamSubmits(@PathVariable Long examId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(examSubmitService.getSubmitList(examId, user.getId()));
    }

    @GetMapping("/detail/{submitId}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "获取考试提交详情(教师)")
    public Result<ExamSubmitVO> getSubmitDetail(@PathVariable Long submitId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(examSubmitService.getSubmitDetail(submitId, user.getId()));
    }

    @PostMapping("/{examId}/grade")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "批改考试主观题")
    public Result<?> gradeExam(@PathVariable Long examId,
                               @Valid @RequestBody ExamGradeRequest request,
                               Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        examSubmitService.gradeExamAnswers(examId, request, user.getId());
        return Result.success();
    }

    @PostMapping("/ai-grade")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "AI评分考试题目（支持填空/简答，支持批量）")
    public Result<List<ExamAiGradeItemResponse>> aiGrade(@Valid @RequestBody ExamAiGradeRequest request,
                                                         Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(examSubmitService.suggestSubjectiveGrades(request, user.getId()));
    }
}



