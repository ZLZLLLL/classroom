package com.classroom.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.QuestionCreateRequest;
import com.classroom.entity.Question;
import com.classroom.exception.BusinessException;
import com.classroom.repository.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService extends ServiceImpl<QuestionMapper, Question> {

    private final CourseService courseService;

    public Question createQuestion(QuestionCreateRequest request, Long teacherId) {
        courseService.assertTeacherOwnsCourse(request.getCourseId(), teacherId);
        Question question = new Question();
        BeanUtils.copyProperties(request, question);
        question.setTeacherId(teacherId);
        question.setStatus(1); // 进行中

        // 序列化选项
        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            question.setOptions(JSONUtil.toJsonStr(request.getOptions()));
        }

        // 序列化班级ID
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            question.setClassIds(JSONUtil.toJsonStr(request.getClassIds()));
        }

        this.save(question);
        return question;
    }

    public Question getActiveQuestion(Long courseId) {
        return this.getOne(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .eq(Question::getStatus, 1)
                .orderByDesc(Question::getCreateTime)
                .last("LIMIT 1"));
    }

    public List<Question> getCourseQuestions(Long courseId) {
        return this.list(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, courseId)
                .orderByDesc(Question::getCreateTime));
    }

    public List<Question> getTeacherQuestions(Long teacherId) {
        return this.list(new LambdaQueryWrapper<Question>()
                .eq(Question::getTeacherId, teacherId)
                .orderByDesc(Question::getCreateTime));
    }

    public Question getQuestionById(Long id) {
        return this.getById(id);
    }

    public void closeQuestion(Long id, Long teacherId) {
        Question question = this.getById(id);
        if (question == null) {
            throw new BusinessException("问题不存在");
        }
        if (!question.getTeacherId().equals(teacherId)
                || !courseService.isTeacherCourseOwner(question.getCourseId(), teacherId)) {
            throw new BusinessException("无权限操作");
        }

        question.setStatus(2); // 已结束
        this.updateById(question);
    }

    public void assertTeacherCanAccessQuestion(Long questionId, Long teacherId) {
        Question question = this.getById(questionId);
        if (question == null) {
            throw new BusinessException("问题不存在");
        }
        if (!courseService.isTeacherCourseOwner(question.getCourseId(), teacherId)) {
            throw new BusinessException("无权限查看该问题");
        }
    }
}
