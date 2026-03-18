package com.classroom.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classroom.common.Result;
import com.classroom.dto.AnnouncementCreateRequest;
import com.classroom.entity.SystemAnnouncement;
import com.classroom.entity.User;
import com.classroom.service.SystemAnnouncementService;
import com.classroom.service.UserService;
import com.classroom.vo.AdminDashboardVO;
import com.classroom.vo.SystemAnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "管理员")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {

    private final UserService userService;
    private final SystemAnnouncementService announcementService;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping("/dashboard/stats")
    @Operation(summary = "管理员首页统计")
    public Result<AdminDashboardVO> getDashboardStats() {
        long total = userService.count(new LambdaQueryWrapper<>());
        long teachers = userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, 1));
        long students = userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, 2));

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptimeMs / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        AdminDashboardVO vo = new AdminDashboardVO();
        vo.setTotalRegistered((int) total);
        vo.setTeacherCount((int) teachers);
        vo.setStudentCount((int) students);
        vo.setUptime(days + "天 " + hours + "小时 " + minutes + "分钟");
        vo.setVersion(appVersion);
        return Result.success(vo);
    }

    @PostMapping("/announcements")
    @Operation(summary = "发布系统公告")
    public Result<SystemAnnouncementVO> publishAnnouncement(@Valid @RequestBody AnnouncementCreateRequest request,
                                                            Authentication authentication) {
        User operator = (User) authentication.getPrincipal();
        SystemAnnouncement announcement = announcementService.publish(request, operator.getId());
        return Result.success(announcementService.toVO(announcement));
    }
}


