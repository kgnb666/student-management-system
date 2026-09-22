package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.score.entity.ClassInfo;
import com.example.score.entity.Course;
import com.example.score.entity.Teacher;
import com.example.score.entity.User;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.ClassInfoMapper;
import com.example.score.mapper.CourseMapper;
import com.example.score.mapper.TeacherMapper;
import com.example.score.mapper.UserMapper;
import com.example.score.vo.PageResult;
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
    private final UserAuthCache userAuthCache;

    public List<Teacher> list(String keyword) {
        return teacherMapper.selectList(new LambdaQueryWrapper<Teacher>()
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(Teacher::getTeacherNo, keyword)
                        .or()
                        .like(Teacher::getName, keyword))
                .orderByAsc(Teacher::getTeacherNo));
    }

    public PageResult<Teacher> listPaged(String keyword, Long page, Long size) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<Teacher>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Teacher::getTeacherNo, keyword)
                        .or()
                        .like(Teacher::getName, keyword))
                .orderByAsc(Teacher::getTeacherNo);
        Page<Teacher> pageParam = new Page<>(PageResult.normalizeCurrent(page), PageResult.normalizeSize(size));
        return PageResult.of(teacherMapper.selectPage(pageParam, wrapper));
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
        checkTeacherNoAvailable(teacher.getTeacherNo(), null, null);
        User user = new User();
        user.setUsername(teacher.getTeacherNo());
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole("TEACHER");
        user.setRealName(teacher.getName());
        user.setStatus(teacher.getStatus() == null ? 1 : teacher.getStatus());
        user.setNeedChangePassword(1);
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
        checkTeacherNoAvailable(teacher.getTeacherNo(), teacher.getId(), oldTeacher.getUserId());
        teacherMapper.updateById(teacher);

        User user = userMapper.selectById(oldTeacher.getUserId());
        if (user != null) {
            user.setUsername(teacher.getTeacherNo());
            user.setRealName(teacher.getName());
            user.setStatus(teacher.getStatus());
            // 账号状态发生变化时让旧 token 立即失效。
            if (teacher.getStatus() != null && !teacher.getStatus().equals(oldTeacher.getStatus())) {
                bumpTokenVersion(user);
            }
            userMapper.updateById(user);
            userAuthCache.evict(user.getId());
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
        userAuthCache.evict(teacher.getUserId());
    }

    public void resetPassword(Long id) {
        Teacher teacher = getById(id);
        User user = userMapper.selectById(teacher.getUserId());
        if (user == null) {
            throw new BusinessException("教师账号不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        // 密码被重置后需要本人修改初始密码，旧 token 同时失效。
        user.setNeedChangePassword(1);
        bumpTokenVersion(user);
        userMapper.updateById(user);
        userAuthCache.evict(user.getId());
    }

    private void bumpTokenVersion(User user) {
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
    }

    /**
     * 改工号/新增教师前先查重，给出明确提示，而不是等数据库唯一键报错。
     */
    private void checkTeacherNoAvailable(String teacherNo, Long excludeTeacherId, Long excludeUserId) {
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, teacherNo)
                .ne(excludeUserId != null, User::getId, excludeUserId));
        if (userCount != null && userCount > 0) {
            throw new BusinessException("该工号已被占用");
        }
        Long teacherCount = teacherMapper.selectCount(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTeacherNo, teacherNo)
                .ne(excludeTeacherId != null, Teacher::getId, excludeTeacherId));
        if (teacherCount != null && teacherCount > 0) {
            throw new BusinessException("该工号已被占用");
        }
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
