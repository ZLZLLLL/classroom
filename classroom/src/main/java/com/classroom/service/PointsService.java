package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.entity.Points;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.PointsMapper;
import com.classroom.repository.UserMapper;
import com.classroom.vo.PointsRankingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointsService extends ServiceImpl<PointsMapper, Points> {

    private final UserMapper userMapper;
    private final CourseService courseService;

    public Integer getUserPoints(Long userId) {
        List<Points> pointsList = this.list(new LambdaQueryWrapper<Points>()
                .eq(Points::getUserId, userId));

        return pointsList.stream()
                .mapToInt(Points::getPoints)
                .sum();
    }

    public List<Points> getUserCoursePointsRecords(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return new ArrayList<>();
        }
        return this.list(new LambdaQueryWrapper<Points>()
                .eq(Points::getUserId, userId)
                .eq(Points::getCourseId, courseId)
                .orderByDesc(Points::getCreateTime));
    }

    public List<PointsRankingVO> getCourseRanking(Long courseId) {
        List<Points> pointsList = this.list(new LambdaQueryWrapper<Points>()
                .eq(Points::getCourseId, courseId));

        // 按userId分组求和
        Map<Long, Integer> userPointsMap = pointsList.stream()
                .collect(Collectors.groupingBy(Points::getUserId,
                        Collectors.summingInt(Points::getPoints)));

        // 获取用户信息（只查询学生）
        List<Long> userIds = new ArrayList<>(userPointsMap.keySet());
        List<User> users = userIds.isEmpty() ? new ArrayList<>() :
                userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, userIds)
                        .eq(User::getRole, 2));
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return userPointsMap.entrySet().stream()
                .map(entry -> {
                    PointsRankingVO vo = new PointsRankingVO();
                    vo.setUserId(entry.getKey());
                    vo.setPoints(entry.getValue());
                    User user = userMap.get(entry.getKey());
                    if (user != null) {
                        vo.setUserName(user.getUsername());
                        vo.setRealName(user.getRealName() == null ? user.getUsername() : user.getRealName());
                        vo.setAvatar(user.getAvatar());
                    } else {
                        vo.setUserName("用户" + entry.getKey());
                        vo.setRealName("用户" + entry.getKey());
                    }
                    return vo;
                })
                .sorted((a, b) -> b.getPoints() - a.getPoints())
                .collect(Collectors.toList());
    }

    @Transactional
    public void addPointsForUsers(List<Long> userIds, Long courseId, Integer points, String description) {
        User operator = getCurrentUserOrThrow();
        if (operator.getRole() == null || operator.getRole() != 1) {
            throw new BusinessException("仅教师可手动加分");
        }
        Long operatorTeacherId = operator.getId();
        courseService.assertTeacherOwnsCourse(courseId, operatorTeacherId);
        for (Long userId : userIds) {
            if (!courseService.isStudentInCourse(courseId, userId)) {
                throw new BusinessException("存在非本课程学生，无法加分");
            }
            Points pointsRecord = new Points();
            pointsRecord.setUserId(userId);
            pointsRecord.setCourseId(courseId);
            pointsRecord.setType(6); // 手动加分
            pointsRecord.setPoints(points);
            pointsRecord.setDescription(description);
            this.save(pointsRecord);
        }
    }

    private User getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new BusinessException("未认证用户");
        }
        return user;
    }
}
