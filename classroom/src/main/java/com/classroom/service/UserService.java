package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;

    public User findByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    public User findByStudentNo(String studentNo) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, studentNo));
    }

    public boolean isValidStudentNo(String studentNo) {
        return studentNo != null && studentNo.matches("^2202\\d{3}\\d{4}$");
    }

    public User register(User user) {
        // 检查用户名是否存在
        User existingUser = this.findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        if (!isValidStudentNo(user.getStudentNo())) {
            throw new BusinessException("学号格式不正确，应为2202开头共11位数字");
        }
        User existedByStudentNo = this.findByStudentNo(user.getStudentNo());
        if (existedByStudentNo != null) {
            throw new BusinessException("学号已存在");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        if (user.getRole() == null) {
            user.setRole(2); // 默认为学生
        }
        if (user.getRole() == 3) {
            throw new BusinessException("不允许注册管理员账号");
        }
        // classId 可以为 null，不设置默认值

        this.save(user);
        return user;
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public Page<User> getManageableUsers(Integer page, Integer size, Integer role, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .ne(User::getRole, 3)
                .orderByDesc(User::getCreateTime);

        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getRealName, keyword)
                    .or()
                    .like(User::getUsername, keyword));
        }

        return this.page(new Page<>(page, size), wrapper);
    }

    public void resetPassword(Long userId, String newPassword, Long operatorId) {
        if (userId == null || newPassword == null || newPassword.isBlank()) {
            throw new BusinessException("重置密码参数不完整");
        }
        User target = this.getById(userId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if (target.getRole() != null && target.getRole() == 3) {
            throw new BusinessException("不支持重置管理员密码");
        }
        if (operatorId != null && operatorId.equals(userId)) {
            throw new BusinessException("请使用个人修改密码功能");
        }
        target.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(target);
    }

    public void updateUserStatus(Long userId, Integer status, Long operatorId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态参数不合法");
        }
        User target = this.getById(userId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if (target.getRole() != null && target.getRole() == 3) {
            throw new BusinessException("不允许封禁管理员");
        }
        if (operatorId != null && operatorId.equals(userId)) {
            throw new BusinessException("不允许封禁当前管理员账号");
        }
        target.setStatus(status);
        this.updateById(target);
    }
}
