package com.classroom.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.entity.Course;
import com.classroom.entity.CourseStudent;
import com.classroom.entity.ExamNotice;
import com.classroom.entity.User;
import com.classroom.repository.CourseMapper;
import com.classroom.repository.CourseStudentMapper;
import com.classroom.repository.ExamNoticeMapper;
import com.classroom.vo.ExamNoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamNoticeService extends ServiceImpl<ExamNoticeMapper, ExamNotice> {

    private final CourseStudentMapper courseStudentMapper;
    private final CourseMapper courseMapper;

    public void createNotice(ExamNotice notice) {
        this.save(notice);
    }

    public List<ExamNoticeVO> getStudentNotices(User user) {
        List<CourseStudent> members = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getUserId, user.getId())
                .eq(CourseStudent::getStatus, 1));

        if (members.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = members.stream()
                .map(CourseStudent::getCourseId)
                .distinct()
                .collect(Collectors.toList());

        List<ExamNotice> notices = this.list(new LambdaQueryWrapper<ExamNotice>()
                .in(ExamNotice::getCourseId, courseIds)
                .orderByDesc(ExamNotice::getCreateTime));

        return notices.stream()
                .filter(notice -> matchesClass(notice, user.getClassId()))
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private boolean matchesClass(ExamNotice notice, Long classId) {
        if (notice.getClassIds() == null || notice.getClassIds().isBlank()) {
            return true;
        }
        if (classId == null) {
            return false;
        }
        List<Long> classIds = JSONUtil.toList(notice.getClassIds(), Long.class);
        return classIds.contains(classId);
    }

    private ExamNoticeVO toVO(ExamNotice notice) {
        ExamNoticeVO vo = new ExamNoticeVO();
        BeanUtils.copyProperties(notice, vo);
        Course course = courseMapper.selectById(notice.getCourseId());
        vo.setCourseName(course == null ? null : course.getName());
        return vo;
    }
}

