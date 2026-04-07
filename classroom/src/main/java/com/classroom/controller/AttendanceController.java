package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.dto.AttendanceCreateRequest;
import com.classroom.dto.SignInRequest;
import com.classroom.entity.Attendance;
import com.classroom.entity.AttendanceActivity;
import com.classroom.entity.User;
import com.classroom.service.AttendanceService;
import com.classroom.vo.AttendanceActivityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "签到管理")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/signIn")
    @Operation(summary = "学生签到")
    public Result<Attendance> signIn(@RequestBody SignInRequest request,
                                      Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Attendance attendance = attendanceService.signIn(request, user.getId());
        return Result.success(attendance);
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "教师发起签到")
    public Result<AttendanceActivity> createAttendance(@RequestBody AttendanceCreateRequest request,
                                                      Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        AttendanceActivity activity = attendanceService.createAttendanceActivity(request, user.getId());
        return Result.success(activity);
    }

    @GetMapping("/course/{courseId}/activities")
    @Operation(summary = "获取课程签到活动列表")
    public Result<List<AttendanceActivityVO>> getCourseActivities(@PathVariable Long courseId) {
        List<AttendanceActivityVO> activities = attendanceService.getCourseActivities(courseId);
        return Result.success(activities);
    }

    @GetMapping("/activity/{activityId}/details")
    @Operation(summary = "获取签到活动详情（已签到/未签到学生）")
    public Result<AttendanceActivityVO> getActivityDetails(@PathVariable Long activityId) {
        AttendanceActivityVO details = attendanceService.getActivityDetails(activityId);
        return Result.success(details);
    }

    @GetMapping("/student/activities")
    @Operation(summary = "获取学生待签到活动")
    public Result<List<AttendanceActivityVO>> getStudentActivities(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<AttendanceActivityVO> activities = attendanceService.getStudentPendingActivities(user.getId());
        return Result.success(activities);
    }

    @GetMapping("/course/{courseId}/today")
    @Operation(summary = "获取今日签到状态")
    public Result<List<Attendance>> getTodayAttendance(@PathVariable Long courseId) {
        List<Attendance> attendances = attendanceService.getTodayAttendance(courseId);
        return Result.success(attendances);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ROLE_TEACHER','ROLE_ADMIN')")
    @Operation(summary = "获取所有签到统计")
    public Result<List<Attendance>> getAllAttendanceStatistics() {
        List<Attendance> attendances = attendanceService.getAllAttendanceStatistics();
        return Result.success(attendances);
    }

    @GetMapping("/course/{courseId}/statistics")
    @Operation(summary = "获取签到统计")
    public Result<List<Attendance>> getAttendanceStatistics(@PathVariable Long courseId) {
        List<Attendance> attendances = attendanceService.getAttendanceStatistics(courseId);
        return Result.success(attendances);
    }

    @GetMapping("/my/statistics")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    @Operation(summary = "获取我的签到统计汇总")
    public Result<Map<String, Integer>> getMyAttendanceStatistics(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return Result.success(attendanceService.getStudentAttendanceSummary(user.getId()));
    }
}
