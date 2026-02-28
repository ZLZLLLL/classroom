package com.classroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeCreateRequest {

    @NotNull(message = "目标用户ID不能为空")
    private Long targetUserId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotNull(message = "类型不能为空")
    private Integer type;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;
}
