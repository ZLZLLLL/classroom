package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.entity.User;
import com.classroom.service.ExamNoticeService;
import com.classroom.vo.ExamNoticeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exam-notices")
@RequiredArgsConstructor
@Tag(name = "考试通知")
public class ExamNoticeController {

    private final ExamNoticeService examNoticeService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "获取我的考试通知")
    public Result<List<ExamNoticeVO>> getMyNotices(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(examNoticeService.getStudentNotices(user));
    }
}

