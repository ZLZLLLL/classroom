package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VoteVO implements Serializable {

    private Long id;

    private Long courseId;

    private String courseName;

    private Long teacherId;

    private String teacherName;

    private String title;

    private Integer status;

    private Integer type;

    private Integer anonymous;

    private List<VoteOptionVO> options;

    private Integer totalVotes;

    private String myOption;

    private List<String> myOptions;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    public static class VoteOptionVO implements Serializable {
        private String key;
        private String content;
        private Integer voteCount;
        private Integer percentage;
        private List<String> voterNames;
    }
}


