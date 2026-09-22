package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.score.entity.Student;
import com.example.score.entity.User;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.StudentMapper;
import com.example.score.mapper.UserMapper;
import com.example.score.vo.StudentVO;
import com.example.score.vo.PageResult;
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
    private final UserAuthCache userAuthCache;

    public List<StudentVO> list(String keyword, Long classId, Integer status) {
        return studentMapper.selectStudentList(keyword, classId, status);
    }

    public PageResult<StudentVO> listPaged(String keyword, Long classId, Integer status, Long page, Long size) {
        Page<StudentVO> pageParam = new Page<>(PageResult.normalizeCurrent(page), PageResult.normalizeSize(size));
        return PageResult.of(studentMapper.selectStudentListPage(pageParam, keyword, classId, status));
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
        saveStudent(student, "123456", true);
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
        saveStudent(student, password, false);
    }

    private void saveStudent(Student student, String password, boolean needChangePassword) {
        checkStudent(student);
        checkStudentNoAvailable(student.getStudentNo(), null, null);
        User user = new User();
        user.setUsername(student.getStudentNo());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("STUDENT");
        user.setRealName(student.getName());
        user.setStatus(student.getStatus() == null ? 1 : student.getStatus());
        user.setNeedChangePassword(needChangePassword ? 1 : 0);
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
        checkStudentNoAvailable(student.getStudentNo(), student.getId(), oldStudent.getUserId());
        studentMapper.updateById(student);

        User user = userMapper.selectById(oldStudent.getUserId());
        if (user != null) {
            user.setUsername(student.getStudentNo());
            user.setRealName(student.getName());
            user.setStatus(student.getStatus());
            // 账号状态发生变化时让旧 token 立即失效。
            if (student.getStatus() != null && !student.getStatus().equals(oldStudent.getStatus())) {
                bumpTokenVersion(user);
            }
            userMapper.updateById(user);
            userAuthCache.evict(user.getId());
        }
    }

    /**
     * 审核通过自助注册的学生账号，使其可以正常登录。
     */
    @Transactional
    public void approve(Long id) {
        Student student = getById(id);
        if (student.getStatus() != null && student.getStatus() == 1) {
            throw new BusinessException("该学生账号已是正常状态");
        }
        student.setStatus(1);
        studentMapper.updateById(student);

        User user = userMapper.selectById(student.getUserId());
        if (user == null) {
            throw new BusinessException("学生账号不存在");
        }
        user.setStatus(1);
        bumpTokenVersion(user);
        userMapper.updateById(user);
        userAuthCache.evict(user.getId());
    }

    @Transactional
    public void delete(Long id) {
        Student student = getById(id);
        studentMapper.deleteById(id);
        userMapper.deleteById(student.getUserId());
        userAuthCache.evict(student.getUserId());
    }

    public void resetPassword(Long id) {
        Student student = getById(id);
        User user = userMapper.selectById(student.getUserId());
        if (user == null) {
            throw new BusinessException("学生账号不存在");
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
     * 改学号/新增学生前先查重，给出明确提示，而不是等数据库唯一键报错。
     *
     * @param excludeStudentId 更新时排除自身的学生 id
     * @param excludeUserId    更新时排除自身的账号 id
     */
    private void checkStudentNoAvailable(String studentNo, Long excludeStudentId, Long excludeUserId) {
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, studentNo)
                .ne(excludeUserId != null, User::getId, excludeUserId));
        if (userCount != null && userCount > 0) {
            throw new BusinessException("该学号已被占用");
        }
        Long studentCount = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, studentNo)
                .ne(excludeStudentId != null, Student::getId, excludeStudentId));
        if (studentCount != null && studentCount > 0) {
            throw new BusinessException("该学号已被占用");
        }
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
