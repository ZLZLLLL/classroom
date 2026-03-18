package com.classroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerReviewRequest {

    @NotNull
    private Long answerId;

    /**
     * 是否判为正确：true=正确；false=错误（简答题通常不做“正确/错误”，但用于统计与展示）
     */
    @NotNull
    private Boolean correct;

    /**
     * 得分：0..1000（建议前端限制 0..question.points）
     */
    @NotNull
    @Min(0)
    @Max(1000)
    private Integer score;
}

