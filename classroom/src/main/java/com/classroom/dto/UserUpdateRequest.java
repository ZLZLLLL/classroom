package com.classroom.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String realName;

    private Long classId;

    private String avatar;

    private String phone;

    private String email;

    private String password;
}
