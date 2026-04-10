package com.classroom.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.classroom.entity.CourseClass;
import com.classroom.entity.CourseStudent;
import com.classroom.dto.UserUpdateRequest;
import com.classroom.entity.Admin;
import com.classroom.entity.Class;
import com.classroom.entity.Student;
import com.classroom.entity.Teacher;
import com.classroom.entity.User;
import com.classroom.exception.BusinessException;
import com.classroom.repository.AdminMapper;
import com.classroom.repository.CourseClassMapper;
import com.classroom.repository.CourseStudentMapper;
import com.classroom.repository.StudentMapper;
import com.classroom.repository.TeacherMapper;
import com.classroom.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final AdminMapper adminMapper;
    private final ClassService classService;
    private final CourseClassMapper courseClassMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final FileService fileService;

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

        if (user.getRole() == 1) {
            Teacher teacher = new Teacher();
            teacher.setUsername(user.getUsername());
            teacher.setPassword(user.getPassword());
            teacher.setRealName(user.getRealName());
            teacher.setStudentNo(user.getStudentNo());
            teacher.setAvatar(user.getAvatar());
            teacher.setPhone(user.getPhone());
            teacher.setEmail(user.getEmail());
            teacher.setStatus(user.getStatus());
            teacherMapper.insert(teacher);
            return this.getById(teacher.getId());
        }

        if (user.getClassId() == null) {
            throw new BusinessException("学生注册时必须绑定班级");
        }
        Class boundClass = classService.getById(user.getClassId());
        if (boundClass == null) {
            throw new BusinessException("班级不存在");
        }

        Student student = new Student();
        student.setUsername(user.getUsername());
        student.setPassword(user.getPassword());
        student.setRealName(user.getRealName());
        student.setStudentNo(user.getStudentNo());
        student.setClassId(user.getClassId());
        student.setAvatar(user.getAvatar());
        student.setPhone(user.getPhone());
        student.setEmail(user.getEmail());
        student.setStatus(user.getStatus());
        studentMapper.insert(student);
        return this.getById(student.getId());
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

    public User updateCurrentUser(Long userId, UserUpdateRequest request) {
        if (userId == null) {
            throw new BusinessException("用户不存在");
        }
        User updateUser = this.getById(userId);
        if (updateUser == null) {
            throw new BusinessException("用户不存在");
        }

        if (request.getRealName() != null) {
            updateUser.setRealName(request.getRealName());
        }
        if (request.getClassId() != null) {
            if (updateUser.getRole() != null && updateUser.getRole() == 2) {
                throw new BusinessException("学生不可自行修改班级");
            }
            updateUser.setClassId(request.getClassId());
        }
        if (request.getAvatar() != null) {
            updateUser.setAvatar(fileService.normalizeStoredPath(request.getAvatar()));
        }
        if (request.getPhone() != null) {
            updateUser.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            updateUser.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            updateUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        this.updateById(updateUser);
        return this.getById(userId);
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

    @Transactional
    public User updateStudentClass(Long userId, Long classId, User operator) {
        if (operator == null || operator.getRole() == null || (operator.getRole() != 1 && operator.getRole() != 3)) {
            throw new BusinessException("无权限操作");
        }
        if (userId == null) {
            throw new BusinessException("参数不完整");
        }

        User target = this.getById(userId);
        if (target == null || target.getRole() == null || target.getRole() != 2) {
            throw new BusinessException("仅支持修改学生班级");
        }
        if (classId != null) {
            Class targetClass = classService.getById(classId);
            if (targetClass == null) {
                throw new BusinessException("班级不存在");
            }
        }

        Long oldClassId = target.getClassId();
        if (Objects.equals(oldClassId, classId)) {
            return target;
        }

        Student student = studentMapper.selectById(userId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        studentMapper.update(null, new LambdaUpdateWrapper<Student>()
                .eq(Student::getId, userId)
                .set(Student::getClassId, classId));

        syncStudentCourseMembershipForClassChange(userId, oldClassId, classId);
        return this.getById(userId);
    }

    private void syncStudentCourseMembershipForClassChange(Long studentId, Long oldClassId, Long newClassId) {
        if (studentId == null || Objects.equals(oldClassId, newClassId)) {
            return;
        }

        if (oldClassId != null) {
            List<Long> oldCourseIds = listCourseIdsByClassId(oldClassId);
            if (!oldCourseIds.isEmpty()) {
                courseStudentMapper.update(null, new LambdaUpdateWrapper<CourseStudent>()
                        .eq(CourseStudent::getUserId, studentId)
                        .eq(CourseStudent::getSourceClassId, oldClassId)
                        .in(CourseStudent::getCourseId, oldCourseIds)
                        .set(CourseStudent::getStatus, 0));
            }
        }

        if (newClassId != null) {
            List<Long> newCourseIds = listCourseIdsByClassId(newClassId);
            for (Long courseId : newCourseIds) {
                CourseStudent existing = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                        .eq(CourseStudent::getCourseId, courseId)
                        .eq(CourseStudent::getUserId, studentId));
                if (existing == null) {
                    CourseStudent member = new CourseStudent();
                    member.setCourseId(courseId);
                    member.setUserId(studentId);
                    member.setSourceClassId(newClassId);
                    member.setStatus(1);
                    courseStudentMapper.insert(member);
                    continue;
                }
                existing.setSourceClassId(newClassId);
                existing.setStatus(1);
                courseStudentMapper.updateById(existing);
            }
        }
    }

    private List<Long> listCourseIdsByClassId(Long classId) {
        if (classId == null) {
            return new ArrayList<>();
        }
        return courseClassMapper.selectList(new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getClassId, classId))
                .stream()
                .map(CourseClass::getCourseId)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
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

    @Override
    public boolean updateById(User entity) {
        if (entity == null || entity.getId() == null) {
            return false;
        }

        Integer role = entity.getRole();
        if (role == null) {
            User existing = this.getById(entity.getId());
            if (existing == null) {
                return false;
            }
            role = existing.getRole();
        }

        if (role == null) {
            return false;
        }

        if (role == 1) {
            Teacher teacher = teacherMapper.selectById(entity.getId());
            if (teacher == null) {
                return false;
            }
            mergeCommonFields(entity, teacher::setPassword, teacher::setRealName, teacher::setAvatar,
                    teacher::setPhone, teacher::setEmail, teacher::setStatus);
            return teacherMapper.updateById(teacher) > 0;
        }

        if (role == 2) {
            Student student = studentMapper.selectById(entity.getId());
            if (student == null) {
                return false;
            }
            mergeCommonFields(entity, student::setPassword, student::setRealName, student::setAvatar,
                    student::setPhone, student::setEmail, student::setStatus);
            if (entity.getClassId() != null) {
                student.setClassId(entity.getClassId());
            }
            return studentMapper.updateById(student) > 0;
        }

        if (role == 3) {
            Admin admin = adminMapper.selectById(entity.getId());
            if (admin == null) {
                return false;
            }
            mergeCommonFields(entity, admin::setPassword, admin::setRealName, admin::setAvatar,
                    admin::setPhone, admin::setEmail, admin::setStatus);
            return adminMapper.updateById(admin) > 0;
        }

        return false;
    }

    private void mergeCommonFields(User source,
                                   java.util.function.Consumer<String> setPassword,
                                   java.util.function.Consumer<String> setRealName,
                                   java.util.function.Consumer<String> setAvatar,
                                   java.util.function.Consumer<String> setPhone,
                                   java.util.function.Consumer<String> setEmail,
                                   java.util.function.Consumer<Integer> setStatus) {
        if (source.getPassword() != null) {
            setPassword.accept(source.getPassword());
        }
        if (source.getRealName() != null) {
            setRealName.accept(source.getRealName());
        }
        if (source.getAvatar() != null) {
            setAvatar.accept(source.getAvatar());
        }
        if (source.getPhone() != null) {
            setPhone.accept(source.getPhone());
        }
        if (source.getEmail() != null) {
            setEmail.accept(source.getEmail());
        }
        if (source.getStatus() != null) {
            setStatus.accept(source.getStatus());
        }
    }
}
