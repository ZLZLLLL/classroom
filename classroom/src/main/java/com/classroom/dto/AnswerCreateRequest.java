package com.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerCreateRequest {

    @NotNull(message = "问题ID不能为空")
    private Long questionId;

    @NotBlank(message = "回答内容不能为空")
    private String content;
}
