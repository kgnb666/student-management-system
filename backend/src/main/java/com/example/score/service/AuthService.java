package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.score.dto.LoginRequest;
import com.example.score.dto.LoginResponse;
import com.example.score.dto.RegisterRequest;
import com.example.score.entity.ClassInfo;
import com.example.score.entity.Student;
import com.example.score.entity.User;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.UserMapper;
import com.example.score.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StudentService studentService;
    private final ClassService classService;

    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("请输入用户名和密码");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException("账号已停用");
        }

        String token = jwtUtil.createToken(user);
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }

    public void register(RegisterRequest request) {
        if (request.getStudentNo() == null || request.getStudentNo().isBlank()) {
            throw new BusinessException("学号不能为空");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("姓名不能为空");
        }
        if (request.getClassId() == null) {
            throw new BusinessException("请选择班级");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException("密码不能少于 6 位");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        classService.getById(request.getClassId());

        Student student = new Student();
        student.setStudentNo(request.getStudentNo());
        student.setName(request.getName());
        student.setGender(request.getGender());
        student.setPhone(request.getPhone());
        student.setClassId(request.getClassId());
        student.setStatus(1);
        studentService.register(student, request.getPassword());
    }

    public List<ClassInfo> listClasses() {
        return classService.list();
    }
}
