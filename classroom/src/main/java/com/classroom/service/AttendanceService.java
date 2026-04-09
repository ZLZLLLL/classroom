package com.classroom.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.AttendanceCreateRequest;
import com.classroom.dto.SignInRequest;
import com.classroom.entity.*;
import com.classroom.exception.BusinessException;
import com.classroom.repository.*;
import com.classroom.vo.AttendanceActivityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService extends ServiceImpl<AttendanceMapper, Attendance> {

    private final PointsMapper pointsMapper;
    private final AttendanceActivityMapper attendanceActivityMapper;
    private final CourseService courseService;
    private final UserMapper userMapper;

    @Transactional
    public Attendance signIn(SignInRequest request, Long userId) {
        // 检查是否在签到活动中
        AttendanceActivity activity = attendanceActivityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new BusinessException("签到活动不存在");
        }
        if (activity.getStatus() != 1) {
            throw new BusinessException("签到已结束");
        }

        // 检查是否在签到时间内
        LocalDateTime expireTime = activity.getCreateTime().plusMinutes(activity.getDuration());
        if (LocalDateTime.now().isAfter(expireTime)) {
            throw new BusinessException("签到已超时");
        }

        // 检查今天是否已经签到（针对同一活动）
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getUserId, userId)
                .eq(Attendance::getCourseId, activity.getCourseId())
                .ge(Attendance::getSignTime, startOfDay)
                .le(Attendance::getSignTime, endOfDay));

        if (count > 0) {
            throw new BusinessException("今日已签到");
        }

        Attendance attendance = new Attendance();
        attendance.setCourseId(activity.getCourseId());
        attendance.setUserId(userId);
        attendance.setSignTime(LocalDateTime.now());
        attendance.setStatus(1); // 成功
        attendance.setLatitude(request.getLatitude());
        attendance.setLongitude(request.getLongitude());

        this.save(attendance);

        // 添加积分 - 签到5分
        addPoints(userId, activity.getCourseId());

        return attendance;
    }

    @Transactional
    public Attendance assistSignIn(com.classroom.dto.AttendanceAssistSignRequest request, Long operatorId) {
        AttendanceActivity activity = attendanceActivityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new BusinessException("签到活动不存在");
        }

        User operator = userMapper.selectById(operatorId);
        if (operator == null || operator.getRole() == null) {
            throw new BusinessException("无权限操作");
        }
        if (operator.getRole() == 1) {
            courseService.assertTeacherOwnsCourse(activity.getCourseId(), operatorId);
        } else if (operator.getRole() != 3) {
            throw new BusinessException("无权限操作");
        }

        User target = userMapper.selectById(request.getUserId());
        if (target == null || target.getRole() == null || target.getRole() != 2) {
            throw new BusinessException("只能为学生补签");
        }
        if (!courseService.isStudentInCourse(activity.getCourseId(), target.getId())) {
            throw new BusinessException("该学生不在课程范围内");
        }

        LocalDateTime startTime = activity.getCreateTime();
        LocalDateTime endTime = activity.getCreateTime().plusMinutes(activity.getDuration());
        Long existed = this.baseMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getUserId, target.getId())
                .eq(Attendance::getCourseId, activity.getCourseId())
                .ge(Attendance::getSignTime, startTime)
                .le(Attendance::getSignTime, endTime));
        if (existed != null && existed > 0) {
            throw new BusinessException("该学生已完成该活动签到");
        }

        Integer status = request.getStatus();
        if (status == null || (status != 1 && status != 2 && status != 3)) {
            throw new BusinessException("签到状态不合法");
        }

        Attendance attendance = new Attendance();
        attendance.setCourseId(activity.getCourseId());
        attendance.setUserId(target.getId());
        attendance.setSignTime(LocalDateTime.now());
        attendance.setStatus(status);
        attendance.setOperatorId(operatorId);
        this.save(attendance);

        if (status == 1) {
            addPoints(target.getId(), activity.getCourseId());
        }
        return attendance;
    }

    @Transactional
    public AttendanceActivity createAttendanceActivity(AttendanceCreateRequest request, Long teacherId) {
        courseService.assertTeacherOwnsCourse(request.getCourseId(), teacherId);
        AttendanceActivity activity = new AttendanceActivity();
        activity.setCourseId(request.getCourseId());
        activity.setTeacherId(teacherId);
        activity.setDuration(request.getDuration());
        activity.setLocation(request.getLocation());
        activity.setStatus(1); // 进行中

        // 序列化班级ID
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            activity.setClassIds(JSONUtil.toJsonStr(request.getClassIds()));
        }

        attendanceActivityMapper.insert(activity);
        return activity;
    }

    public List<AttendanceActivityVO> getCourseActivities(Long courseId) {
        assertCanAccessCourseAttendance(courseId, getCurrentUserOrThrow());
        List<AttendanceActivity> activities = attendanceActivityMapper.selectList(
                new LambdaQueryWrapper<AttendanceActivity>()
                        .eq(AttendanceActivity::getCourseId, courseId)
                        .orderByDesc(AttendanceActivity::getCreateTime)
        );

        Course course = courseService.getCourseById(courseId);
        String courseName = course != null ? course.getName() : "";

        LocalDateTime now = LocalDateTime.now();
        List<AttendanceActivityVO> result = new ArrayList<>();
        for (AttendanceActivity activity : activities) {
            AttendanceActivityVO vo = new AttendanceActivityVO();
            BeanUtils.copyProperties(activity, vo);
            vo.setCourseName(courseName);

            // 动态计算状态：超过签到时长则视为已结束
            LocalDateTime endTime = activity.getCreateTime().plusMinutes(activity.getDuration());
            if (now.isAfter(endTime)) {
                vo.setStatus(2); // 已结束
            } else {
                vo.setStatus(1); // 进行中
            }

            // 获取该活动的签到统计
            fillActivityStatistics(vo, activity);
            result.add(vo);
        }
        return result;
    }

    public AttendanceActivityVO getActivityDetails(Long activityId) {
        AttendanceActivity activity = attendanceActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("签到活动不存在");
        }
        assertCanAccessCourseAttendance(activity.getCourseId(), getCurrentUserOrThrow());

        AttendanceActivityVO vo = new AttendanceActivityVO();
        BeanUtils.copyProperties(activity, vo);

        Course course = courseService.getCourseById(activity.getCourseId());
        if (course != null) {
            vo.setCourseName(course.getName());
        }

        User teacher = userMapper.selectById(activity.getTeacherId());
        if (teacher != null) {
            vo.setTeacherName(teacher.getRealName());
        }

        // 获取所有学生
        List<User> students = courseService.getCourseStudents(activity.getCourseId());

        // 获取已签到学生（限定在签到活动时间范围内）
        LocalDateTime startTime = activity.getCreateTime();
        LocalDateTime endTime = activity.getCreateTime().plusMinutes(activity.getDuration());
        List<Attendance> attendances = this.list(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getCourseId, activity.getCourseId())
                .ge(Attendance::getSignTime, startTime)
                .le(Attendance::getSignTime, endTime));

        Map<Long, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(Attendance::getUserId, a -> a, (a, b) -> a));

        List<AttendanceActivityVO.StudentSignInfo> signed = new ArrayList<>();
        List<AttendanceActivityVO.StudentSignInfo> unsigned = new ArrayList<>();

        for (User student : students) {
            AttendanceActivityVO.StudentSignInfo info = new AttendanceActivityVO.StudentSignInfo();
            info.setUserId(student.getId());
            info.setStudentNo(student.getStudentNo());
            info.setUserName(student.getUsername());
            info.setRealName(student.getRealName());

            Attendance att = attendanceMap.get(student.getId());
            if (att != null) {
                info.setSignTime(att.getSignTime());
                info.setStatus(att.getStatus());
                signed.add(info);
            } else {
                unsigned.add(info);
            }
        }

        vo.setSignedStudents(signed);
        vo.setUnsignedStudents(unsigned);
        vo.setTotalStudents(students.size());
        vo.setSignedCount(signed.size());
        vo.setUnsignedCount(unsigned.size());

        return vo;
    }

    public List<AttendanceActivityVO> getStudentPendingActivities(Long userId) {
        // 获取学生所在的班级
        User student = userMapper.selectById(userId);
        if (student == null || student.getClassId() == null) {
            return new ArrayList<>();
        }

        // 获取该学生班级的所有进行中的签到活动
        List<AttendanceActivity> activities = attendanceActivityMapper.selectList(
                new LambdaQueryWrapper<AttendanceActivity>()
                        .eq(AttendanceActivity::getStatus, 1)
                        .orderByDesc(AttendanceActivity::getCreateTime)
        );

        List<AttendanceActivityVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AttendanceActivity activity : activities) {
            // 检查是否在签到时间内
            LocalDateTime expireTime = activity.getCreateTime().plusMinutes(activity.getDuration());
            if (now.isAfter(expireTime)) {
                continue;
            }

            // 检查学生是否在目标班级中
            if (activity.getClassIds() != null && !activity.getClassIds().isEmpty()) {
                List<Long> classIds = JSONUtil.toList(activity.getClassIds(), Long.class);
                if (!classIds.contains(student.getClassId())) {
                    continue;
                }
            }

            // 检查是否已签到
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

            Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                    .eq(Attendance::getUserId, userId)
                    .eq(Attendance::getCourseId, activity.getCourseId())
                    .ge(Attendance::getSignTime, startOfDay)
                    .le(Attendance::getSignTime, endOfDay));

            if (count > 0) {
                continue; // 已签到，跳过
            }

            AttendanceActivityVO vo = new AttendanceActivityVO();
            BeanUtils.copyProperties(activity, vo);

            Course course = courseService.getCourseById(activity.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getName());
            }

            result.add(vo);
        }

        return result;
    }

    private void fillActivityStatistics(AttendanceActivityVO vo, AttendanceActivity activity) {
        // 获取课程学生
        List<User> students = courseService.getCourseStudents(activity.getCourseId());
        vo.setTotalStudents(students.size());

        // 获取该签到活动时间段内的签到记录
        LocalDateTime startTime = activity.getCreateTime();
        LocalDateTime endTime = activity.getCreateTime().plusMinutes(activity.getDuration());

        List<Attendance> attendances = this.list(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getCourseId, activity.getCourseId())
                .ge(Attendance::getSignTime, startTime)
                .le(Attendance::getSignTime, endTime));

        vo.setSignedCount(attendances.size());
        vo.setUnsignedCount(students.size() - attendances.size());
    }

    public List<Attendance> getTodayAttendance(Long courseId) {
        assertCanAccessCourseAttendance(courseId, getCurrentUserOrThrow());
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        return this.list(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getCourseId, courseId)
                .ge(Attendance::getSignTime, startOfDay)
                .le(Attendance::getSignTime, endOfDay));
    }

    public List<Attendance> getAttendanceStatistics(Long courseId) {
        assertCanAccessCourseAttendance(courseId, getCurrentUserOrThrow());
        return this.list(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getCourseId, courseId)
                .orderByDesc(Attendance::getSignTime));
    }

    public Map<String, Integer> getStudentAttendanceSummary(Long userId) {
        User student = userMapper.selectById(userId);
        Map<String, Integer> summary = new HashMap<>();
        summary.put("totalActivities", 0);
        summary.put("signedActivities", 0);
        summary.put("rate", 0);
        if (student == null || student.getRole() == null || student.getRole() != 2) {
            return summary;
        }

        List<Long> courseIds = courseService.getStudentCourseIds(userId);
        if (courseIds.isEmpty()) {
            return summary;
        }

        List<AttendanceActivity> allActivities = attendanceActivityMapper.selectList(new LambdaQueryWrapper<AttendanceActivity>()
                .in(AttendanceActivity::getCourseId, courseIds));
        List<AttendanceActivity> eligibleActivities = allActivities.stream()
                .filter(activity -> {
                    if (activity.getClassIds() == null || activity.getClassIds().isBlank()) {
                        return true;
                    }
                    if (student.getClassId() == null) {
                        return false;
                    }
                    List<Long> classIds = JSONUtil.toList(activity.getClassIds(), Long.class);
                    return classIds.contains(student.getClassId());
                })
                .toList();

        int totalActivities = eligibleActivities.size();
        int signedActivities = (int) this.count(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getUserId, userId)
                .eq(Attendance::getStatus, 1)
                .in(Attendance::getCourseId, courseIds));
        int rate = totalActivities == 0 ? 0 : (int) Math.round(signedActivities * 100.0 / totalActivities);

        summary.put("totalActivities", totalActivities);
        summary.put("signedActivities", signedActivities);
        summary.put("rate", Math.max(0, Math.min(100, rate)));
        return summary;
    }

    public List<Attendance> getAllAttendanceStatistics() {
        User actor = getCurrentUserOrThrow();
        if (actor == null || actor.getRole() == null) {
            throw new BusinessException("无权限查看签到统计");
        }
        if (actor.getRole() == 3) {
            return this.list(new LambdaQueryWrapper<Attendance>()
                    .orderByDesc(Attendance::getSignTime));
        }
        if (actor.getRole() == 1) {
            List<Course> teacherCourses = courseService.list(new LambdaQueryWrapper<Course>()
                    .eq(Course::getTeacherId, actor.getId()));
            List<Long> courseIds = teacherCourses.stream().map(Course::getId).toList();
            if (courseIds.isEmpty()) {
                return new ArrayList<>();
            }
            return this.list(new LambdaQueryWrapper<Attendance>()
                    .in(Attendance::getCourseId, courseIds)
                    .orderByDesc(Attendance::getSignTime));
        }
        throw new BusinessException("无权限查看签到统计");
    }

    private void assertCanAccessCourseAttendance(Long courseId, User actor) {
        if (actor == null || actor.getRole() == null) {
            throw new BusinessException("无权限查看签到信息");
        }
        if (actor.getRole() == 3) {
            return;
        }
        if (actor.getRole() == 1 && courseService.isTeacherCourseOwner(courseId, actor.getId())) {
            return;
        }
        if (actor.getRole() == 2 && courseService.isStudentInCourse(courseId, actor.getId())) {
            return;
        }
        throw new BusinessException("无权限查看该课程签到信息");
    }

    private User getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new BusinessException("未认证用户");
        }
        return user;
    }

    private void addPoints(Long userId, Long courseId) {
        Points pointsRecord = new Points();
        pointsRecord.setUserId(userId);
        pointsRecord.setCourseId(courseId);
        pointsRecord.setType(1);
        pointsRecord.setPoints(5);
        pointsRecord.setDescription("签到");
        pointsMapper.insert(pointsRecord);
    }
}
