package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.score.entity.ClassInfo;
import com.example.score.entity.Course;
import com.example.score.entity.Teacher;
import com.example.score.entity.User;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.ClassInfoMapper;
import com.example.score.mapper.CourseMapper;
import com.example.score.mapper.TeacherMapper;
import com.example.score.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final ClassInfoMapper classInfoMapper;
    private final PasswordEncoder passwordEncoder;

    public List<Teacher> list(String keyword) {
        return teacherMapper.selectList(new LambdaQueryWrapper<Teacher>()
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(Teacher::getTeacherNo, keyword)
                        .or()
                        .like(Teacher::getName, keyword))
                .orderByAsc(Teacher::getTeacherNo));
    }

    public Teacher getByUserId(Long userId) {
        Teacher teacher = teacherMapper.selectOne(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getUserId, userId));
        if (teacher == null) {
            throw new BusinessException("教师信息不存在");
        }
        return teacher;
    }

    public Teacher getById(Long id) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new BusinessException("教师不存在");
        }
        return teacher;
    }

    @Transactional
    public void create(Teacher teacher) {
        checkTeacher(teacher);
        User user = new User();
        user.setUsername(teacher.getTeacherNo());
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("TEACHER");
        user.setRealName(teacher.getName());
        user.setStatus(teacher.getStatus() == null ? 1 : teacher.getStatus());
        userMapper.insert(user);

        teacher.setUserId(user.getId());
        if (teacher.getStatus() == null) {
            teacher.setStatus(1);
        }
        teacherMapper.insert(teacher);
    }

    @Transactional
    public void update(Teacher teacher) {
        checkTeacher(teacher);
        Teacher oldTeacher = getById(teacher.getId());
        teacherMapper.updateById(teacher);

        User user = userMapper.selectById(oldTeacher.getUserId());
        if (user != null) {
            user.setUsername(teacher.getTeacherNo());
            user.setRealName(teacher.getName());
            user.setStatus(teacher.getStatus());
            userMapper.updateById(user);
        }
    }

    @Transactional
    public void delete(Long id) {
        Teacher teacher = getById(id);
        Long courseCount = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getTeacherId, id));
        if (courseCount > 0) {
            throw new BusinessException("该教师仍有授课课程，不能删除");
        }
        Long classCount = classInfoMapper.selectCount(new LambdaQueryWrapper<ClassInfo>()
                .eq(ClassInfo::getHeadTeacherId, id));
        if (classCount > 0) {
            throw new BusinessException("该教师仍是班级班主任，不能删除");
        }

        teacherMapper.deleteById(id);
        userMapper.deleteById(teacher.getUserId());
    }

    public void resetPassword(Long id) {
        Teacher teacher = getById(id);
        User user = userMapper.selectById(teacher.getUserId());
        if (user == null) {
            throw new BusinessException("教师账号不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
    }

    private void checkTeacher(Teacher teacher) {
        if (teacher.getTeacherNo() == null || teacher.getTeacherNo().isBlank()) {
            throw new BusinessException("工号不能为空");
        }
        if (teacher.getName() == null || teacher.getName().isBlank()) {
            throw new BusinessException("姓名不能为空");
        }
    }
}
