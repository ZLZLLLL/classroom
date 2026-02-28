package com.classroom.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;

    private String studentNo;

    @NotNull(message = "角色不能为空")
    private Integer role;

    private Long classId;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;
}
