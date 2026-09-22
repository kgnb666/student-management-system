package com.example.score;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口级集成测试：登录、越权、成绩锁定与分页。
 *
 * <p>测试方法带事务，所有数据改动都会回滚；MySQL 不可用时自动跳过。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void requireDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "MySQL 不可用，跳过接口集成测试：" + e.getMessage());
        }
        // 演示账号可能被管理员停用或重置过密码，测试前临时恢复为可用状态；
        // 该方法运行在测试事务内，改动会随事务回滚，不影响真实数据。
        jdbcTemplate.update("UPDATE sys_user SET status = 1, need_change_password = 0 "
                + "WHERE username IN ('admin', 't001', '2023001')");
    }

    @Test
    @DisplayName("三种角色都能登录并返回对应角色")
    void shouldLoginForAllRoles() throws Exception {
        assertThat(loginAndGetRole("admin", "admin123")).isEqualTo("ADMIN");
        assertThat(loginAndGetRole("t001", "123456")).isEqualTo("TEACHER");
        assertThat(loginAndGetRole("2023001", "123456")).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("密码错误返回 400 且提示不区分账号是否存在")
    void shouldRejectWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("未携带 token 返回 401")
    void shouldRejectWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("学生访问管理员接口返回 403")
    void shouldRejectStudentAccessingAdminApi() throws Exception {
        String token = login("2023001", "123456");

        mockMvc.perform(get("/api/admin/students").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("成绩提交锁定后教师不能再修改，解锁后恢复")
    void shouldBlockModificationAfterSubmit() throws Exception {
        String teacherToken = login("t001", "123456");
        String adminToken = login("admin", "admin123");
        String batchBody = "{\"courseId\":1,\"scores\":[{\"studentId\":1,\"usualScore\":80,\"examScore\":90}]}";

        mockMvc.perform(post("/api/teacher/courses/1/submit")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/teacher/scores/batch")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该课程成绩已提交锁定，如需修改请联系管理员解锁"));

        mockMvc.perform(post("/api/admin/courses/1/unlock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/teacher/scores/batch")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("管理员列表分页参数生效，不传分页时保持数组结构")
    void shouldSupportPagination() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(get("/api/admin/students?page=1&size=3").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(8))
                .andExpect(jsonPath("$.data.records.length()").value(3))
                .andExpect(jsonPath("$.data.current").value(1));

        mockMvc.perform(get("/api/admin/students").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(8));
    }

    @Test
    @DisplayName("学生绩点接口按 4.0 制返回结果")
    void shouldReturnStudentGpa() throws Exception {
        String token = login("2023001", "123456");

        mockMvc.perform(get("/api/student/gpa?semesterId=2").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gpa").value(3.64))
                .andExpect(jsonPath("$.data.totalCredit").value(14.0));
    }

    @Test
    @DisplayName("非法成绩被参数校验拦截")
    void shouldRejectInvalidScore() throws Exception {
        String token = login("admin", "admin123");

        mockMvc.perform(post("/api/admin/scores")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":1,\"studentId\":1,\"usualScore\":120,\"examScore\":90}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("成绩必须在 0 到 100 之间"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data").path("token").asText();
    }

    private String loginAndGetRole(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data").path("role").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
