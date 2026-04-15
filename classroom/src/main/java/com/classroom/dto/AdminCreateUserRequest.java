package com.classroom.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;

    @NotBlank(message = "学号/工号不能为空")
    @Pattern(regexp = "^2202\\d{3}\\d{4}$", message = "学号/工号格式不正确，应为2202开头共11位数字")
    private String studentNo;

    /**
     * 1-教师 2-学生
     */
    @NotNull(message = "角色不能为空")
    private Integer role;

    private Long classId;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;
}

