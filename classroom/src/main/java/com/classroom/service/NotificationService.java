package com.classroom.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送签到通知给学生
     */
    @SuppressWarnings("unused")
    public void sendSignInNotification(Long courseId, String courseName) {
        messagingTemplate.convertAndSend("/topic/course/" + courseId + "/signin", new NotificationMessage(
                "签到提醒",
                "您有新的签到任务：" + courseName,
                "signin",
                courseId
        ));
    }

    /**
     * 发送提问通知给学生
     */
    @SuppressWarnings("unused")
    public void sendQuestionNotification(Long courseId, String courseName, String questionContent) {
        messagingTemplate.convertAndSend("/topic/course/" + courseId + "/question", new NotificationMessage(
                "课堂提问",
                courseName + "：有新的提问：" + questionContent,
                "question",
                courseId
        ));
    }

    /**
     * 发送作业通知给学生
     */
    @SuppressWarnings("unused")
    public void sendHomeworkNotification(Long courseId, String courseName, String homeworkTitle) {
        messagingTemplate.convertAndSend("/topic/course/" + courseId + "/homework", new NotificationMessage(
                "新作业",
                courseName + "：发布了新作业：" + homeworkTitle,
                "homework",
                courseId
        ));
    }

    /**
     * 发送考试通知给学生
     */
    public void sendExamNotification(Long courseId, String courseName, String examTitle) {
        messagingTemplate.convertAndSend("/topic/course/" + courseId + "/exam", new NotificationMessage(
                "新考试",
                courseName + "：发布了考试：" + examTitle,
                "exam",
                courseId
        ));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationMessage {
        private String title;
        private String message;
        private String type;
        private Long courseId;
    }
}
