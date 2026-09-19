package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.score.entity.Student;
import com.example.score.entity.User;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.StudentMapper;
import com.example.score.mapper.UserMapper;
import com.example.score.vo.StudentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<StudentVO> list(String keyword, Long classId) {
        return studentMapper.selectStudentList(keyword, classId);
    }

    public Student getByUserId(Long userId) {
        return studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId));
    }

    public Student getById(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }
        return student;
    }

    public StudentVO getProfileByUserId(Long userId) {
        Student student = getByUserId(userId);
        if (student == null) {
            throw new BusinessException("学生信息不存在");
        }
        return studentMapper.selectStudentVO(student.getId());
    }

    @Transactional
    public void create(Student student) {
        saveStudent(student, "123456");
    }

    @Transactional
    public void register(Student student, String password) {
        if (password == null || password.length() < 6) {
            throw new BusinessException("密码不能少于 6 位");
        }
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, student.getStudentNo()));
        if (existingUser != null || studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, student.getStudentNo())) != null) {
            throw new BusinessException("该学号已注册");
        }
        saveStudent(student, password);
    }

    private void saveStudent(Student student, String password) {
        checkStudent(student);
        User user = new User();
        user.setUsername(student.getStudentNo());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("STUDENT");
        user.setRealName(student.getName());
        user.setStatus(student.getStatus() == null ? 1 : student.getStatus());
        userMapper.insert(user);

        student.setUserId(user.getId());
        if (student.getStatus() == null) {
            student.setStatus(1);
        }
        studentMapper.insert(student);
    }

    @Transactional
    public void update(Student student) {
        checkStudent(student);
        Student oldStudent = getById(student.getId());
        studentMapper.updateById(student);

        User user = userMapper.selectById(oldStudent.getUserId());
        if (user != null) {
            user.setUsername(student.getStudentNo());
            user.setRealName(student.getName());
            user.setStatus(student.getStatus());
            userMapper.updateById(user);
        }
    }

    @Transactional
    public void delete(Long id) {
        Student student = getById(id);
        studentMapper.deleteById(id);
        userMapper.deleteById(student.getUserId());
    }

    public void resetPassword(Long id) {
        Student student = getById(id);
        User user = userMapper.selectById(student.getUserId());
        if (user == null) {
            throw new BusinessException("学生账号不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
    }

    private void checkStudent(Student student) {
        if (student.getStudentNo() == null || student.getStudentNo().isBlank()) {
            throw new BusinessException("学号不能为空");
        }
        if (student.getName() == null || student.getName().isBlank()) {
            throw new BusinessException("姓名不能为空");
        }
        if (student.getClassId() == null) {
            throw new BusinessException("请选择班级");
        }
    }
}
