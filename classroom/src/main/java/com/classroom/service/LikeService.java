package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.LikeCreateRequest;
import com.classroom.entity.Answer;
import com.classroom.entity.Like;
import com.classroom.entity.Points;
import com.classroom.entity.Question;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.AnswerMapper;
import com.classroom.repository.LikeMapper;
import com.classroom.repository.PointsMapper;
import com.classroom.repository.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService extends ServiceImpl<LikeMapper, Like> {

    private final PointsMapper pointsMapper;
    private final AnswerMapper answerMapper;
    private final QuestionMapper questionMapper;
    private final UserService userService;
    private final CourseService courseService;

    @Transactional
    public Like addLike(LikeCreateRequest request, Long userId) {
        if (request.getType() == null || request.getType() != 1) {
            throw new BusinessException("仅支持回答点赞");
        }

        User liker = userService.getById(userId);
        if (liker == null || liker.getRole() == null || liker.getRole() != 2) {
            throw new BusinessException("仅学生可点赞回答");
        }

        Answer answer = answerMapper.selectById(request.getTargetId());
        if (answer == null) {
            throw new BusinessException("回答不存在");
        }

        Question question = questionMapper.selectById(answer.getQuestionId());
        if (question == null) {
            throw new BusinessException("问题不存在");
        }

        if (!courseService.isStudentInCourse(question.getCourseId(), userId)) {
            throw new BusinessException("无权限点赞该课程回答");
        }

        if (answer.getUserId() != null && answer.getUserId().equals(userId)) {
            throw new BusinessException("不能给自己的回答点赞");
        }

        // 进行中的提问：需先完成自己的回答再点赞，避免提前查看/互动影响作答。
        if (question.getStatus() != null && question.getStatus() == 1) {
            Long mine = answerMapper.selectCount(new LambdaQueryWrapper<Answer>()
                    .eq(Answer::getQuestionId, question.getId())
                    .eq(Answer::getUserId, userId));
            if (mine == null || mine == 0) {
                throw new BusinessException("请先提交自己的回答再点赞");
            }
        }

        // 检查是否已经点赞
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<Like>()
                .eq(Like::getUserId, userId)
                .eq(Like::getType, 1)
                .eq(Like::getTargetId, answer.getId()));

        if (count > 0) {
            throw new BusinessException("您已经点赞过");
        }

        Like like = new Like();
        like.setUserId(userId);
        like.setTargetUserId(answer.getUserId());
        like.setCourseId(question.getCourseId());
        like.setType(1);
        like.setTargetId(answer.getId());

        this.save(like);

        // 给被点赞用户添加积分
        Points points = new Points();
        points.setUserId(answer.getUserId());
        points.setCourseId(question.getCourseId());
        points.setType(3); // 点赞
        points.setPoints(1);
        points.setDescription("回答被点赞");
        pointsMapper.insert(points);

        return like;
    }

    public void removeLike(Long id, Long userId) {
        Like like = this.getById(id);
        if (like == null) {
            throw new BusinessException("点赞不存在");
        }
        if (!like.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除");
        }
        this.removeById(id);
    }

    public Long getAnswerLikeCount(Long answerId) {
        return this.baseMapper.selectCount(new LambdaQueryWrapper<Like>()
                .eq(Like::getType, 1)
                .eq(Like::getTargetId, answerId));
    }

    @SuppressWarnings("unused")
    public Like getMyAnswerLike(Long answerId, Long userId) {
        return this.getOne(new LambdaQueryWrapper<Like>()
                .eq(Like::getType, 1)
                .eq(Like::getTargetId, answerId)
                .eq(Like::getUserId, userId));
    }
}
