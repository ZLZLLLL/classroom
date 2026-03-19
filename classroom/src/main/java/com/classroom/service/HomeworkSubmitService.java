package com.classroom.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.HomeworkGradingRequest;
import com.classroom.dto.HomeworkSubmitRequest;
import com.classroom.entity.Homework;
import com.classroom.entity.HomeworkSubmit;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.HomeworkSubmitMapper;
import com.classroom.repository.UserMapper;
import com.classroom.vo.HomeworkPendingStudentVO;
import com.classroom.vo.HomeworkSubmitStatusVO;
import com.classroom.vo.HomeworkSubmitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeworkSubmitService extends ServiceImpl<HomeworkSubmitMapper, HomeworkSubmit> {

    private final HomeworkService homeworkService;
    private final UserMapper userMapper;
    private final CourseService courseService;

    @Transactional
    public HomeworkSubmit submitHomework(Long homeworkId, HomeworkSubmitRequest request, Long userId) {
        if (request == null || ((request.getContent() == null || request.getContent().isBlank())
                && (request.getFilePath() == null || request.getFilePath().isBlank()))) {
            throw new BusinessException("提交内容不能为空");
        }

        HomeworkSubmit submit = this.getOne(new LambdaQueryWrapper<HomeworkSubmit>()
                .eq(HomeworkSubmit::getHomeworkId, homeworkId)
                .eq(HomeworkSubmit::getUserId, userId));

        if (submit != null) {
            throw new BusinessException("您已提交过作业");
        }

        // 获取作业信息
        Homework homework = homeworkService.getHomeworkById(homeworkId);
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }

        if (homework.getDeadline() != null && LocalDateTime.now().isAfter(homework.getDeadline())) {
            throw new BusinessException("已超过作业截止时间");
        }

        var user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 2) {
            throw new BusinessException("仅学生可提交作业");
        }
        if (user.getClassId() == null) {
            throw new BusinessException("学生未绑定班级，无法提交作业");
        }

        List<Long> courseClassIds = courseService.getCourseClasses(homework.getCourseId()).stream()
                .map(com.classroom.entity.Class::getId)
                .collect(Collectors.toList());
        if (!courseClassIds.contains(user.getClassId())) {
            throw new BusinessException("您不在该课程的班级范围内");
        }

        // 若作业指定了目标班级，则必须在目标班级内才能提交。
        if (homework.getClassIds() != null && !homework.getClassIds().isBlank()) {
            List<Long> classIds = JSONUtil.toList(homework.getClassIds(), Long.class);
            if (user.getClassId() == null || !classIds.contains(user.getClassId())) {
                throw new BusinessException("当前作业不面向您所在班级");
            }
        }

        submit = new HomeworkSubmit();
        submit.setHomeworkId(homeworkId);
        submit.setUserId(userId);
        submit.setContent(request.getContent());
        submit.setFilePath(request.getFilePath());
        submit.setSubmitTime(LocalDateTime.now());
        submit.setStatus(1); // 待批改

        this.save(submit);

        return submit;
    }

    public List<HomeworkSubmit> getHomeworkSubmits(Long homeworkId, Long teacherId) {
        Homework homework = homeworkService.getHomeworkById(homeworkId);
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        if (!homework.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限查看该作业提交");
        }

        return this.list(new LambdaQueryWrapper<HomeworkSubmit>()
                .eq(HomeworkSubmit::getHomeworkId, homeworkId)
                .orderByDesc(HomeworkSubmit::getSubmitTime));
    }

    public HomeworkSubmit getMySubmit(Long homeworkId, Long userId) {
        return this.getOne(new LambdaQueryWrapper<HomeworkSubmit>()
                .eq(HomeworkSubmit::getHomeworkId, homeworkId)
                .eq(HomeworkSubmit::getUserId, userId));
    }

    @Transactional
    public HomeworkSubmit gradeHomework(Long submitId, HomeworkGradingRequest request, Long teacherId) {
        HomeworkSubmit submit = this.getById(submitId);
        if (submit == null) {
            throw new BusinessException("提交记录不存在");
        }

        Homework homework = homeworkService.getHomeworkById(submit.getHomeworkId());
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        if (!homework.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限批改该作业");
        }

        if (request.getScore() == null || request.getScore() < 0) {
            throw new BusinessException("分数不能小于0");
        }
        if (homework.getTotalPoints() != null && request.getScore() > homework.getTotalPoints()) {
            throw new BusinessException("分数不能超过作业总分");
        }

        submit.setScore(request.getScore());
        submit.setFeedback(request.getFeedback());
        submit.setStatus(2); // 已批改

        this.updateById(submit);

        return submit;
    }

    public HomeworkSubmitStatusVO getHomeworkSubmitStatus(Long homeworkId, Long teacherId) {
        Homework homework = homeworkService.getHomeworkById(homeworkId);
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        if (!homework.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限查看该作业提交");
        }

        List<HomeworkSubmit> submits = this.list(new LambdaQueryWrapper<HomeworkSubmit>()
                .eq(HomeworkSubmit::getHomeworkId, homeworkId)
                .orderByDesc(HomeworkSubmit::getSubmitTime));

        List<User> students = courseService.getCourseStudents(homework.getCourseId());
        if (homework.getClassIds() != null && !homework.getClassIds().isBlank()) {
            List<Long> classIds = JSONUtil.toList(homework.getClassIds(), Long.class);
            students = students.stream()
                    .filter(u -> u.getClassId() != null && classIds.contains(u.getClassId()))
                    .collect(Collectors.toList());
        }

        var submittedUserIds = submits.stream()
                .map(HomeworkSubmit::getUserId)
                .collect(Collectors.toSet());

        var classNameMap = courseService.getCourseClasses(homework.getCourseId()).stream()
                .collect(Collectors.toMap(com.classroom.entity.Class::getId, com.classroom.entity.Class::getName, (a, b) -> a));

        List<HomeworkSubmitVO> submitted = submits.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        List<HomeworkPendingStudentVO> notSubmitted = students.stream()
                .filter(u -> !submittedUserIds.contains(u.getId()))
                .map(u -> {
                    HomeworkPendingStudentVO vo = new HomeworkPendingStudentVO();
                    vo.setUserId(u.getId());
                    vo.setUserName(u.getRealName() != null ? u.getRealName() : u.getUsername());
                    vo.setClassId(u.getClassId());
                    vo.setClassName(u.getClassId() == null ? null : classNameMap.get(u.getClassId()));
                    return vo;
                })
                .collect(Collectors.toList());

        HomeworkSubmitStatusVO status = new HomeworkSubmitStatusVO();
        status.setSubmitted(submitted);
        status.setNotSubmitted(notSubmitted);
        return status;
    }

    private HomeworkSubmitVO convertToVO(HomeworkSubmit submit) {
        HomeworkSubmitVO vo = new HomeworkSubmitVO();
        org.springframework.beans.BeanUtils.copyProperties(submit, vo);
        User user = userMapper.selectById(submit.getUserId());
        if (user != null) {
            vo.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        }
        return vo;
    }
}
