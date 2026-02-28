package com.classroom.service;

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
    public void sendHomeworkNotification(Long courseId, String courseName, String homeworkTitle) {
        messagingTemplate.convertAndSend("/topic/course/" + courseId + "/homework", new NotificationMessage(
                "新作业",
                courseName + "：发布了新作业：" + homeworkTitle,
                "homework",
                courseId
        ));
    }

    /**
     * 通知消息类
     */
    public static class NotificationMessage {
        private String title;
        private String message;
        private String type;
        private Long courseId;

        public NotificationMessage() {}

        public NotificationMessage(String title, String message, String type, Long courseId) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.courseId = courseId;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getCourseId() { return courseId; }
        public void setCourseId(Long courseId) { this.courseId = courseId; }
    }
}
