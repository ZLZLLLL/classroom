package com.classroom.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.ExamCreateRequest;
import com.classroom.dto.ExamQuestionRequest;
import com.classroom.entity.Course;
import com.classroom.entity.Exam;
import com.classroom.entity.ExamNotice;
import com.classroom.entity.ExamQuestion;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.ExamMapper;
import com.classroom.repository.ExamQuestionMapper;
import com.classroom.repository.UserMapper;
import com.classroom.vo.ExamDetailVO;
import com.classroom.vo.ExamQuestionVO;
import com.classroom.vo.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService extends ServiceImpl<ExamMapper, Exam> {

    private final ExamQuestionMapper examQuestionMapper;
    private final CourseService courseService;
    private final UserMapper userMapper;
    private final ExamNoticeService examNoticeService;
    private final NotificationService notificationService;

    @Transactional
    public Exam createExam(ExamCreateRequest request, Long teacherId) {
        courseService.assertTeacherOwnsCourse(request.getCourseId(), teacherId);
        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new BusinessException("题目列表不能为空");
        }
        if (request.getStartTime() != null && request.getEndTime() != null
                && request.getEndTime().isBefore(request.getStartTime())) {
            throw new BusinessException("截止时间不能早于开始时间");
        }

        Exam exam = new Exam();
        BeanUtils.copyProperties(request, exam);
        exam.setTeacherId(teacherId);
        exam.setStatus(1);
        if (exam.getTotalPoints() == null) {
            int sum = request.getQuestions().stream()
                    .map(ExamQuestionRequest::getPoints)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            exam.setTotalPoints(sum > 0 ? sum : 100);
        }

        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            exam.setClassIds(JSONUtil.toJsonStr(request.getClassIds()));
        }

        this.save(exam);

        int index = 1;
        for (ExamQuestionRequest questionRequest : request.getQuestions()) {
            ExamQuestion question = new ExamQuestion();
            BeanUtils.copyProperties(questionRequest, question);
            question.setExamId(exam.getId());
            if (questionRequest.getOptions() != null && !questionRequest.getOptions().isEmpty()) {
                question.setOptions(JSONUtil.toJsonStr(questionRequest.getOptions()));
            }
            if (question.getSortOrder() == null) {
                question.setSortOrder(index);
            }
            examQuestionMapper.insert(question);
            index++;
        }

        return exam;
    }

    @Transactional
    public Exam publishExam(Long examId, Long teacherId) {
        Exam exam = getExamOrThrow(examId);
        if (!exam.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限发布");
        }
        exam.setStatus(2);
        this.updateById(exam);

        Course course = courseService.getById(exam.getCourseId());
        String courseName = course == null ? "" : course.getName();
        String title = "考试通知: " + exam.getTitle();
        String content = courseName + " 发布了考试，截止时间: " + (exam.getEndTime() == null ? "不限" : exam.getEndTime());

        ExamNotice notice = new ExamNotice();
        notice.setExamId(exam.getId());
        notice.setCourseId(exam.getCourseId());
        notice.setTitle(title);
        notice.setContent(content);
        notice.setClassIds(exam.getClassIds());
        examNoticeService.createNotice(notice);

        notificationService.sendExamNotification(exam.getCourseId(), courseName, exam.getTitle());
        return exam;
    }

    public Page<Exam> getTeacherExams(Long teacherId, Integer page, Integer size) {
        return this.page(new Page<>(page, size), new LambdaQueryWrapper<Exam>()
                .eq(Exam::getTeacherId, teacherId)
                .orderByDesc(Exam::getCreateTime));
    }

    public Page<Exam> getStudentExams(Long studentId, Integer page, Integer size, Long courseId) {
        List<Long> courseIds = courseService.getStudentCourseIds(studentId);
        if (courseIds.isEmpty()) {
            return new Page<>(page, size);
        }

        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .in(Exam::getCourseId, courseIds)
                .orderByDesc(Exam::getCreateTime);
        if (courseId != null && courseId > 0) {
            wrapper.eq(Exam::getCourseId, courseId);
        }
        return this.page(new Page<>(page, size), wrapper);
    }

    public ExamDetailVO getExamDetail(Long examId, boolean includeAnswer) {
        Exam exam = getExamOrThrow(examId);
        ExamDetailVO detailVO = new ExamDetailVO();
        detailVO.setExam(toExamVO(exam));

        List<ExamQuestionVO> questions = examQuestionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, examId)
                .orderByAsc(ExamQuestion::getSortOrder))
                .stream()
                .map(q -> toQuestionVO(q, includeAnswer))
                .collect(Collectors.toList());
        detailVO.setQuestions(questions);
        return detailVO;
    }

    public Exam getExamOrThrow(Long examId) {
        Exam exam = this.getById(examId);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        return exam;
    }

    public ExamVO toExamVO(Exam exam) {
        ExamVO vo = new ExamVO();
        BeanUtils.copyProperties(exam, vo);
        Course course = courseService.getById(exam.getCourseId());
        vo.setCourseName(course == null ? null : course.getName());
        User teacher = userMapper.selectById(exam.getTeacherId());
        vo.setTeacherName(teacher == null ? null : (teacher.getRealName() != null ? teacher.getRealName() : teacher.getUsername()));
        return vo;
    }

    public ExamQuestionVO toQuestionVO(ExamQuestion question, boolean includeAnswer) {
        ExamQuestionVO vo = new ExamQuestionVO();
        BeanUtils.copyProperties(question, vo);
        if (question.getOptions() != null && !question.getOptions().isBlank()) {
            vo.setOptions(JSONUtil.toList(question.getOptions(), ExamQuestionVO.Option.class));
        }
        if (!includeAnswer) {
            vo.setCorrectAnswer(null);
            vo.setExplanation(null);
        }
        return vo;
    }

    public boolean isExamActive(Exam exam) {
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStatus() == null || exam.getStatus() != 2) {
            return false;
        }
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            return false;
        }
        return exam.getEndTime() == null || !now.isAfter(exam.getEndTime());
    }
}

