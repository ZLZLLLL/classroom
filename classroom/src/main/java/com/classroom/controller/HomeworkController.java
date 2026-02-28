package com.classroom.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.classroom.common.Result;
import com.classroom.dto.HomeworkCreateRequest;
import com.classroom.entity.Homework;
import com.classroom.entity.User;
import com.classroom.service.HomeworkService;
import com.classroom.vo.HomeworkVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/homeworks")
@RequiredArgsConstructor
@Tag(name = "作业管理")
public class HomeworkController {

    private final HomeworkService homeworkService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "创建作业")
    public Result<HomeworkVO> createHomework(@Valid @RequestBody HomeworkCreateRequest request,
                                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Homework homework = homeworkService.createHomework(request, user.getId());
        return Result.success(convertToVO(homework));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "获取课程作业列表")
    public Result<Page<HomeworkVO>> getCourseHomeworks(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Homework> homeworkPage = homeworkService.getCourseHomeworks(courseId, page, size);

        Page<HomeworkVO> result = new Page<>();
        BeanUtils.copyProperties(homeworkPage, result, "records");
        result.setRecords(homeworkPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(java.util.stream.Collectors.toList()));

        return Result.success(result);
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "获取教师所有作业")
    public Result<Page<HomeworkVO>> getTeacherHomeworks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Page<Homework> homeworkPage = homeworkService.getTeacherHomeworks(user.getId(), page, size);

        Page<HomeworkVO> result = new Page<>();
        BeanUtils.copyProperties(homeworkPage, result, "records");
        result.setRecords(homeworkPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(java.util.stream.Collectors.toList()));

        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取作业详情")
    public Result<HomeworkVO> getHomeworkById(@PathVariable Long id) {
        Homework homework = homeworkService.getHomeworkById(id);
        if (homework == null) {
            return Result.notFound("作业不存在");
        }
        return Result.success(convertToVO(homework));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "更新作业")
    public Result<HomeworkVO> updateHomework(@PathVariable Long id,
                                             @RequestBody HomeworkCreateRequest request,
                                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        homeworkService.updateHomework(id, request, user.getId());
        Homework homework = homeworkService.getHomeworkById(id);
        return Result.success(convertToVO(homework));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "删除作业")
    public Result<?> deleteHomework(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        homeworkService.deleteHomework(id, user.getId());
        return Result.success();
    }

    private HomeworkVO convertToVO(Homework homework) {
        HomeworkVO vo = new HomeworkVO();
        BeanUtils.copyProperties(homework, vo);
        return vo;
    }
}
