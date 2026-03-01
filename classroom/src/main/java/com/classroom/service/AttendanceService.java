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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
        addPoints(userId, activity.getCourseId(), 1, 5, "签到");

        return attendance;
    }

    @Transactional
    public AttendanceActivity createAttendanceActivity(AttendanceCreateRequest request, Long teacherId) {
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
        List<Long> studentIds = students.stream().map(User::getId).collect(Collectors.toList());

        // 获取已签到学生（限定在签到活动时间范围内）
        LocalDateTime startTime = activity.getCreateTime();
        LocalDateTime endTime = activity.getCreateTime().plusMinutes(activity.getDuration());
        List<Attendance> attendances = this.list(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getCourseId, activity.getCourseId())
                .eq(Attendance::getStatus, 1)
                .ge(Attendance::getSignTime, startTime)
                .le(Attendance::getSignTime, endTime));

        Map<Long, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(Attendance::getUserId, a -> a, (a, b) -> a));

        List<AttendanceActivityVO.StudentSignInfo> signed = new ArrayList<>();
        List<AttendanceActivityVO.StudentSignInfo> unsigned = new ArrayList<>();

        for (User student : students) {
            AttendanceActivityVO.StudentSignInfo info = new AttendanceActivityVO.StudentSignInfo();
            info.setUserId(student.getId());
            info.setUserName(student.getUsername());
            info.setRealName(student.getRealName());

            Attendance att = attendanceMap.get(student.getId());
            if (att != null) {
                info.setSignTime(att.getSignTime());
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
                .eq(Attendance::getStatus, 1)
                .ge(Attendance::getSignTime, startTime)
                .le(Attendance::getSignTime, endTime));

        vo.setSignedCount(attendances.size());
        vo.setUnsignedCount(students.size() - attendances.size());
    }

    public List<Attendance> getTodayAttendance(Long courseId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        return this.list(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getCourseId, courseId)
                .ge(Attendance::getSignTime, startOfDay)
                .le(Attendance::getSignTime, endOfDay));
    }

    public List<Attendance> getAttendanceStatistics(Long courseId) {
        return this.list(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getCourseId, courseId)
                .orderByDesc(Attendance::getSignTime));
    }

    public List<Attendance> getAllAttendanceStatistics() {
        return this.list(new LambdaQueryWrapper<Attendance>()
                .orderByDesc(Attendance::getSignTime));
    }

    private void addPoints(Long userId, Long courseId, Integer type, Integer points, String description) {
        Points pointsRecord = new Points();
        pointsRecord.setUserId(userId);
        pointsRecord.setCourseId(courseId);
        pointsRecord.setType(type);
        pointsRecord.setPoints(points);
        pointsRecord.setDescription(description);
        pointsMapper.insert(pointsRecord);
    }
}
