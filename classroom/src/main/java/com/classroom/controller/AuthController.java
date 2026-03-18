package com.classroom.controller;

import com.classroom.common.Result;
import com.classroom.dto.LoginRequest;
import com.classroom.dto.RegisterRequest;
import com.classroom.entity.User;
import com.classroom.security.JwtUtils;
import com.classroom.service.UserService;
import com.classroom.vo.LoginVO;
import com.classroom.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理")
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByStudentNo(request.getStudentNo());
        if (user == null) {
            return Result.unauthorized("学号或密码错误");
        }
        //testAction11
        if (user.getStatus() == 0) {
            return Result.unauthorized("账号已被禁用");
        }

        if (!userService.checkPassword(user, request.getPassword())) {
            return Result.unauthorized("学号或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(convertToVO(user));

        return Result.success(loginVO);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);

        User savedUser = userService.register(user);
        return Result.success(convertToVO(savedUser));
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
