package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AttendanceActivityVO implements Serializable {

    private Long id;

    private Long courseId;

    private String courseName;

    private Long teacherId;

    private String teacherName;

    private Integer duration;

    private String location;

    private Integer status;

    private LocalDateTime createTime;

    private Integer totalStudents;

    private Integer signedCount;

    private Integer unsignedCount;

    private List<StudentSignInfo> signedStudents;

    private List<StudentSignInfo> unsignedStudents;

    @Data
    public static class StudentSignInfo implements Serializable {
        private Long userId;
        private String userName;
        private String realName;
        private LocalDateTime signTime;
    }
}
