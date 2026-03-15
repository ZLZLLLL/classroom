package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.AnswerCreateRequest;
import com.classroom.entity.Answer;
import com.classroom.entity.Points;
import com.classroom.entity.Question;
import com.classroom.exception.BusinessException;
import com.classroom.repository.AnswerMapper;
import com.classroom.repository.PointsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService extends ServiceImpl<AnswerMapper, Answer> {

    private final PointsMapper pointsMapper;
    private final QuestionService questionService;

    @Transactional
    public Answer createAnswer(AnswerCreateRequest request, Long userId) {
        Question question = questionService.getById(request.getQuestionId());
        if (question == null) {
            throw new BusinessException("问题不存在");
        }
        if (question.getStatus() != 1) {
            throw new BusinessException("问题已结束");
        }

        //测试github action
        // 检查是否已经回答过
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, request.getQuestionId())
                .eq(Answer::getUserId, userId));

        if (count > 0) {
            throw new BusinessException("您已回答过该问题");
        }

        Answer answer = new Answer();
        answer.setQuestionId(request.getQuestionId());
        answer.setUserId(userId);
        answer.setContent(request.getContent());

        // 判断是否正确
        if (question.getCorrectAnswer() != null && question.getCorrectAnswer().equals(request.getContent())) {
            answer.setIsCorrect(1);
            answer.setScore(question.getPoints());

            // 添加积分 - 课堂提问2分
            Points points = new Points();
            points.setUserId(userId);
            points.setCourseId(question.getCourseId());
            points.setType(2); // 回答
            points.setPoints(2); // 课堂提问2分
            points.setDescription("回答问题得分");
            pointsMapper.insert(points);
        } else {
            answer.setIsCorrect(2);
            answer.setScore(0);
            // 回答问题即使错误也加2分参与分
            Points points = new Points();
            points.setUserId(userId);
            points.setCourseId(question.getCourseId());
            points.setType(2); // 回答
            points.setPoints(2); // 课堂提问2分
            points.setDescription("回答问题参与分");
            pointsMapper.insert(points);
        }

        this.save(answer);
        return answer;
    }

    public List<Answer> getQuestionAnswers(Long questionId) {
        return this.list(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, questionId)
                .orderByDesc(Answer::getCreateTime));
    }

    public Answer getMyAnswer(Long questionId, Long userId) {
        return this.getOne(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, questionId)
                .eq(Answer::getUserId, userId));
    }
}
