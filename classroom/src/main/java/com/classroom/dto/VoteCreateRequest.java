package com.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class VoteCreateRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "投票标题不能为空")
    private String title;

    @NotNull(message = "投票选项不能为空")
    private List<Option> options;

    private List<Long> classIds;

    /**
     * 类型: 1-单选 2-多选
     */
    private Integer type;

    /**
     * 是否匿名
     */
    private Boolean anonymous;

    @Data
    public static class Option {
        private String key;
        private String content;
    }
}


