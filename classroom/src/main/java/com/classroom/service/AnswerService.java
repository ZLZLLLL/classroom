package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.AnswerCreateRequest;
import com.classroom.dto.AnswerReviewRequest;
import com.classroom.dto.AiGradeSuggestionResponse;
import com.classroom.entity.Answer;
import com.classroom.entity.Points;
import com.classroom.entity.Question;
import com.classroom.exception.BusinessException;
import com.classroom.repository.AnswerMapper;
import com.classroom.repository.PointsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService extends ServiceImpl<AnswerMapper, Answer> {

    private final PointsMapper pointsMapper;
    private final QuestionService questionService;
    private final ClassroomAiService classroomAiService;

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

        // 判题与计分：
        // - 客观题(单选/多选/填空)：判对才加分，得分=question.points
        // - 主观题(简答)：不自动判分，等待教师阅卷
        boolean autoJudge = question.getType() != null && question.getType() != 4;
        if (!autoJudge) {
            // 2 表示未判/待批改（沿用原有字段含义：1正确，2非正确/未判）
            answer.setIsCorrect(2);
            answer.setScore(0);
            this.save(answer);
            return answer;
        }

        String correct = normalizeAnswer(question.getType(), question.getCorrectAnswer());
        String submitted = normalizeAnswer(question.getType(), request.getContent());
        boolean isCorrect = correct != null && !correct.isEmpty() && correct.equals(submitted);

        if (isCorrect) {
            answer.setIsCorrect(1);
            int score = question.getPoints() == null ? 0 : question.getPoints();
            answer.setScore(score);

            Points points = new Points();
            points.setUserId(userId);
            points.setCourseId(question.getCourseId());
            points.setType(2); // 回答
            points.setPoints(score);
            points.setDescription("回答问题得分");
            pointsMapper.insert(points);
        } else {
            answer.setIsCorrect(2);
            answer.setScore(0);
            // 错误不加分（避免与题目分值不一致、避免“参与分”导致排行榜失真）
        }

        this.save(answer);
        return answer;
    }

    private String normalizeAnswer(Integer questionType, String answer) {
        if (answer == null) {
            return null;
        }
        String trimmed = answer.trim();

        // type: 1-单选 2-多选 3-填空 4-简答
        if (questionType != null && questionType == 2) {
            // 多选：统一大小写、去空格，支持逗号/中文逗号/空格分隔，排序后比较
            return Arrays.stream(trimmed
                            .replace('，', ',')
                            .split("[,\\s]+"))
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .sorted()
                    .collect(Collectors.joining(","));
        }

        // 单选/填空：忽略首尾空格；单选统一大写
        if (questionType != null && questionType == 1) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        return trimmed;
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

    @Transactional
    public Answer reviewAnswer(AnswerReviewRequest request, Long teacherId) {
        Answer answer = this.getById(request.getAnswerId());
        if (answer == null) {
            throw new BusinessException("回答不存在");
        }
        Question question = questionService.getById(answer.getQuestionId());
        if (question == null) {
            throw new BusinessException("问题不存在");
        }
        if (!question.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限阅卷");
        }

        int score = request.getScore() == null ? 0 : request.getScore();
        if (question.getPoints() != null && score > question.getPoints()) {
            throw new BusinessException("得分不能超过题目分值");
        }

        answer.setScore(score);
        answer.setIsCorrect(Boolean.TRUE.equals(request.getCorrect()) ? 1 : 2);
        this.updateById(answer);

        // 阅卷加分：避免重复打分导致重复积分（用 description 做最小幂等标识）
        String desc = "简答题阅卷得分#" + answer.getId();
        Long existed = pointsMapper.selectCount(new LambdaQueryWrapper<Points>()
                .eq(Points::getUserId, answer.getUserId())
                .eq(Points::getCourseId, question.getCourseId())
                .eq(Points::getType, 2)
                .eq(Points::getDescription, desc));
        if (existed == null || existed == 0) {
            Points points = new Points();
            points.setUserId(answer.getUserId());
            points.setCourseId(question.getCourseId());
            points.setType(2); // 仍归类为“回答”
            points.setPoints(score);
            points.setDescription(desc);
            pointsMapper.insert(points);
        }

        return answer;
    }

    public AiGradeSuggestionResponse suggestSubjectiveGrade(Long answerId, Long teacherId) {
        Answer answer = this.getById(answerId);
        if (answer == null) {
            throw new BusinessException("回答不存在");
        }
        Question question = questionService.getById(answer.getQuestionId());
        if (question == null) {
            throw new BusinessException("问题不存在");
        }
        if (!question.getTeacherId().equals(teacherId)) {
            throw new BusinessException("无权限阅卷");
        }
        if (question.getType() == null || question.getType() != 4) {
            throw new BusinessException("仅支持简答题评分建议");
        }

        return classroomAiService.suggestSubjectiveGrade(question, answer);
    }
}
