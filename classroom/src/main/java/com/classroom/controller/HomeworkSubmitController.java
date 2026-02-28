package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.dto.HomeworkGradingRequest;
import com.classroom.dto.HomeworkSubmitRequest;
import com.classroom.entity.HomeworkSubmit;
import com.classroom.entity.User;
import com.classroom.service.HomeworkSubmitService;
import com.classroom.vo.HomeworkSubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/homework-submits")
@RequiredArgsConstructor
@Tag(name = "作业提交管理")
public class HomeworkSubmitController {

    private final HomeworkSubmitService homeworkSubmitService;

    @PostMapping("/homework/{homeworkId}")
    @Operation(summary = "提交作业")
    public Result<HomeworkSubmitVO> submitHomework(@PathVariable Long homeworkId,
                                                    @RequestBody HomeworkSubmitRequest request,
                                                    Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        HomeworkSubmit submit = homeworkSubmitService.submitHomework(homeworkId, request, user.getId());
        return Result.success(convertToVO(submit));
    }

    @GetMapping("/homework/{homeworkId}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "获取作业提交列表(教师)")
    public Result<List<HomeworkSubmitVO>> getHomeworkSubmits(@PathVariable Long homeworkId) {
        List<HomeworkSubmit> submits = homeworkSubmitService.getHomeworkSubmits(homeworkId);
        return Result.success(submits.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/homework/{homeworkId}/my")
    @Operation(summary = "获取我的提交")
    public Result<HomeworkSubmitVO> getMySubmit(@PathVariable Long homeworkId,
                                                 Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        HomeworkSubmit submit = homeworkSubmitService.getMySubmit(homeworkId, user.getId());
        if (submit == null) {
            return Result.success(null);
        }
        return Result.success(convertToVO(submit));
    }

    @PutMapping("/{id}/grade")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "批改作业")
    public Result<HomeworkSubmitVO> gradeHomework(@PathVariable Long id,
                                                   @RequestBody HomeworkGradingRequest request) {
        HomeworkSubmit submit = homeworkSubmitService.gradeHomework(id, request);
        return Result.success(convertToVO(submit));
    }

    private HomeworkSubmitVO convertToVO(HomeworkSubmit submit) {
        HomeworkSubmitVO vo = new HomeworkSubmitVO();
        BeanUtils.copyProperties(submit, vo);
        return vo;
    }
}
