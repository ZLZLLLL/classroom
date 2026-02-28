package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String realName;

    private String studentNo;

    private Integer role;

    private Long classId;

    private String className;

    private String avatar;

    private String phone;

    private String email;

    private Integer status;

    private LocalDateTime createTime;
}
