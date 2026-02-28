package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.dto.AddPointsRequest;
import com.classroom.entity.User;
import com.classroom.service.PointsService;
import com.classroom.vo.PointsRankingVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
@Tag(name = "积分管理")
public class PointsController {

    private final PointsService pointsService;

    @GetMapping("/my")
    @Operation(summary = "获取我的积分")
    public Result<Integer> getMyPoints(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Integer points = pointsService.getUserPoints(user.getId());
        return Result.success(points);
    }

    @GetMapping("/course/{courseId}/ranking")
    @Operation(summary = "获取课程积分排行")
    public Result<List<PointsRankingVO>> getCourseRanking(@PathVariable Long courseId) {
        List<PointsRankingVO> ranking = pointsService.getCourseRanking(courseId);
        return Result.success(ranking);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    @Operation(summary = "教师手动给学生加分")
    public Result<?> addPointsForUsers(@RequestBody AddPointsRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        pointsService.addPointsForUsers(request.getUserIds(), request.getCourseId(), request.getPoints(), request.getDescription());
        return Result.success();
    }
}
