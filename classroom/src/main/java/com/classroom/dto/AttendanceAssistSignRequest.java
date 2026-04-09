package com.classroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceAssistSignRequest {

    @NotNull(message = "签到活动不能为空")
    private Long activityId;

    @NotNull(message = "学生不能为空")
    private Long userId;

    /**
     * 状态: 1-签到 2-病假 3-事假
     */
    @NotNull(message = "签到状态不能为空")
    private Integer status;
}

