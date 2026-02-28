package com.classroom.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginVO implements Serializable {

    private String token;

    private UserVO user;
}
