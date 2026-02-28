package com.classroom.controller;

import com.classroom.entity.User;
import com.classroom.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public TestController(PasswordEncoder passwordEncoder, UserService userService) {
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @GetMapping("/encode/{password}")
    public String encode(@PathVariable String password) {
        return passwordEncoder.encode(password);
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        User user = userService.findByUsername(username);
        if (user == null) {
            return Map.of("success", false, "message", "用户不存在");
        }
        boolean matches = passwordEncoder.matches(password, user.getPassword());
        return Map.of("success", matches, "message", matches ? "密码正确" : "密码错误", "hash", user.getPassword());
    }

    @PostMapping("/update-password")
    public Map<String, Object> updatePassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        User user = userService.findByUsername(username);
        if (user == null) {
            return Map.of("success", false, "message", "用户不存在");
        }
        user.setPassword(passwordEncoder.encode(password));
        userService.updateById(user);
        return Map.of("success", true, "message", "密码已更新", "newHash", user.getPassword());
    }
}
