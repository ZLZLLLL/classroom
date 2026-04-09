package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PointsRankingVO implements Serializable {

    private Long userId;

    private String userName;

    private String studentNo;

    private String realName;

    private String avatar;

    private Integer points;
}
