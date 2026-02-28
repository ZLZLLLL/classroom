package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.dto.HomeworkGradingRequest;
import com.classroom.dto.HomeworkSubmitRequest;
import com.classroom.entity.Homework;
import com.classroom.entity.HomeworkSubmit;
import com.classroom.entity.Points;
import com.classroom.exception.BusinessException;
import com.classroom.repository.HomeworkSubmitMapper;
import com.classroom.repository.PointsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeworkSubmitService extends ServiceImpl<HomeworkSubmitMapper, HomeworkSubmit> {

    private final PointsMapper pointsMapper;
    private final HomeworkService homeworkService;

    @Transactional
    public HomeworkSubmit submitHomework(Long homeworkId, HomeworkSubmitRequest request, Long userId) {
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

        submit = new HomeworkSubmit();
        submit.setHomeworkId(homeworkId);
        submit.setUserId(userId);
        submit.setContent(request.getContent());
        submit.setFilePath(request.getFilePath());
        submit.setSubmitTime(LocalDateTime.now());
        submit.setStatus(1); // 待批改

        this.save(submit);

        // 添加积分 - 提交作业3分
        Points points = new Points();
        points.setUserId(userId);
        points.setCourseId(homework.getCourseId());
        points.setType(4); // 作业
        points.setPoints(3); // 作业3分
        points.setDescription("提交作业");
        pointsMapper.insert(points);

        return submit;
    }

    public List<HomeworkSubmit> getHomeworkSubmits(Long homeworkId) {
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
    public HomeworkSubmit gradeHomework(Long submitId, HomeworkGradingRequest request) {
        HomeworkSubmit submit = this.getById(submitId);
        if (submit == null) {
            throw new BusinessException("提交记录不存在");
        }

        submit.setScore(request.getScore());
        submit.setFeedback(request.getFeedback());
        submit.setStatus(2); // 已批改

        this.updateById(submit);

        // 添加积分
        Points points = new Points();
        points.setUserId(submit.getUserId());
        points.setType(4); // 作业
        points.setPoints(request.getScore());
        points.setDescription("作业得分");
        pointsMapper.insert(points);

        return submit;
    }
}
