package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.classroom.entity.Exam;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExamAutoSubmitScheduler {

    private final ExamService examService;
    private final ExamSubmitService examSubmitService;

    @Scheduled(fixedDelay = 60000)
    public void autoSubmitExpiredExams() {
        List<Exam> expired = examService.list(new LambdaQueryWrapper<Exam>()
                .eq(Exam::getStatus, 2)
                .isNotNull(Exam::getEndTime)
                .le(Exam::getEndTime, LocalDateTime.now()));

        for (Exam exam : expired) {
            examSubmitService.autoSubmitExpired(exam);
        }
    }
}

