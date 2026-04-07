package com.classroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.classroom.common.Result;
import com.classroom.dto.ResetPasswordRequest;
import com.classroom.dto.UpdateUserStatusRequest;
import com.classroom.dto.UserUpdateRequest;
import com.classroom.entity.Class;
import com.classroom.entity.User;
import com.classroom.service.ClassService;
import com.classroom.service.FileService;
import com.classroom.service.UserService;
import com.classroom.vo.UserVO;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;
    private final ClassService classService;
    private final FileService fileService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getCurrentUser(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(convertToVO(user));
    }

    @PutMapping("/me")
    @Operation(summary = "更新当前用户信息")
    public Result<UserVO> updateCurrentUser(Authentication authentication,
                                            @RequestBody UserUpdateRequest request) {
        User user = (User) authentication.getPrincipal();
        User updateUser = userService.updateCurrentUser(user.getId(), request);
        return Result.success(convertToVO(updateUser));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取指定用户信息")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.notFound("用户不存在");
        }
        return Result.success(convertToVO(user));
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "获取学生列表(教师)")
    public Result<Page<UserVO>> getStudentList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<User> userPage = userService.page(new Page<>(page, size),
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, 2)
                        .like(keyword != null, User::getRealName, keyword)
                        .or()
                        .like(keyword != null, User::getUsername, keyword));

        Page<UserVO> result = new Page<>();
        BeanUtils.copyProperties(userPage, result, "records");
        result.setRecords(userPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return Result.success(result);
    }

    @GetMapping("/studentsByClass/{classId}")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "获取班级学生列表")
    public Result<List<UserVO>> getStudentsByClass(@PathVariable Long classId) {
        List<User> students = userService.list(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 2)
                .eq(User::getClassId, classId));

        return Result.success(students.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "管理员获取用户列表")
    public Result<Page<UserVO>> getManageableUsers(@RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestParam(required = false) Integer role,
                                                   @RequestParam(required = false) String keyword) {
        Page<User> userPage = userService.getManageableUsers(page, size, role, keyword);

        Page<UserVO> result = new Page<>();
        BeanUtils.copyProperties(userPage, result, "records");
        result.setRecords(userPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));
        return Result.success(result);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "管理员重置用户密码")
    public Result<?> resetPassword(@PathVariable Long id,
                                   @Valid @RequestBody ResetPasswordRequest request,
                                   Authentication authentication) {
        User operator = (User) authentication.getPrincipal();
        userService.resetPassword(id, request.getNewPassword(), operator.getId());
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "管理员封禁/解封用户")
    public Result<?> updateUserStatus(@PathVariable Long id,
                                      @Valid @RequestBody UpdateUserStatusRequest request,
                                      Authentication authentication) {
        User operator = (User) authentication.getPrincipal();
        userService.updateUserStatus(id, request.getStatus(), operator.getId());
        return Result.success();
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setAvatar(fileService.resolveDisplayUrl(user.getAvatar()));

        if (user.getClassId() != null) {
            Class aClass = classService.getById(user.getClassId());
            if (aClass != null) {
                vo.setClassName(aClass.getName());
            }
        }

        return vo;
    }
}
