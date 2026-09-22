package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.score.dto.LoginRequest;
import com.example.score.dto.LoginResponse;
import com.example.score.dto.RegisterRequest;
import com.example.score.dto.ChangePasswordRequest;
import com.example.score.entity.Student;
import com.example.score.entity.User;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.UserMapper;
import com.example.score.util.JwtUtil;
import com.example.score.vo.ClassOptionVO;
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
    private final LoginAttemptService loginAttemptService;
    private final UserAuthCache userAuthCache;

    /** 学生自助注册后的状态：等待管理员审核激活。 */
    private static final int STATUS_PENDING = 2;
    /** 账号正常可用状态。 */
    private static final int STATUS_ACTIVE = 1;

    public LoginResponse login(LoginRequest request, String clientIp) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("请输入用户名和密码");
        }

        String username = request.getUsername().trim();
        loginAttemptService.checkNotLocked(username, clientIp);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(username, clientIp);
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != STATUS_ACTIVE) {
            throw new BusinessException(user.getStatus() != null && user.getStatus() == STATUS_PENDING
                    ? "账号待管理员审核激活，请稍后再试"
                    : "账号已停用");
        }

        loginAttemptService.reset(username, clientIp);
        String token = jwtUtil.createToken(user);
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole(),
                user.getNeedChangePassword() == null ? 0 : user.getNeedChangePassword());
    }

    /**
     * 自助修改密码。改密成功后 token_version 自增，旧 token 立即失效，需要重新登录。
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setNeedChangePassword(0);
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
        userAuthCache.evict(user.getId());
    }

    public void register(RegisterRequest request) {
        if (request.getStudentNo() == null || request.getStudentNo().isBlank()) {
            throw new BusinessException("学号不能为空");
        }
        if (request.getStudentNo().trim().length() > 30) {
            throw new BusinessException("学号长度不能超过 30 个字符");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException("姓名不能为空");
        }
        if (request.getName().trim().length() > 50) {
            throw new BusinessException("姓名长度不能超过 50 个字符");
        }
        if (request.getPhone() != null && request.getPhone().trim().length() > 20) {
            throw new BusinessException("联系电话长度不能超过 20 个字符");
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
        student.setStudentNo(request.getStudentNo().trim());
        student.setName(request.getName().trim());
        student.setGender(request.getGender());
        student.setPhone(request.getPhone() == null ? null : request.getPhone().trim());
        student.setClassId(request.getClassId());
        // 自助注册的账号不能立即登录，需管理员审核通过后才会激活。
        student.setStatus(STATUS_PENDING);
        studentService.register(student, request.getPassword());
    }

    public List<ClassOptionVO> listClasses() {
        return classService.list().stream()
                .map(item -> new ClassOptionVO(item.getId(), item.getClassName()))
                .toList();
    }
}
