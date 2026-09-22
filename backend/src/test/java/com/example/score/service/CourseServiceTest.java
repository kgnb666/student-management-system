package com.example.score.service;

import com.example.score.entity.Course;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.CourseMapper;
import com.example.score.mapper.CourseStudentMapper;
import com.example.score.mapper.ScoreMapper;
import com.example.score.mapper.StudentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成绩提交锁定与管理员解锁的状态流转测试。
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CourseStudentMapper courseStudentMapper;

    @Mock
    private ScoreMapper scoreMapper;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private ScoreChangeLogService scoreChangeLogService;

    @InjectMocks
    private CourseService courseService;

    @Test
    @DisplayName("录入中的课程可以提交锁定")
    void shouldSubmitWhenEditable() {
        when(courseMapper.selectById(1L)).thenReturn(course(0));

        courseService.submitScores(1L, 1L);

        verify(courseMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("已提交的课程不能重复提交")
    void shouldRejectDuplicateSubmit() {
        when(courseMapper.selectById(1L)).thenReturn(course(1));

        assertThatThrownBy(() -> courseService.submitScores(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已提交");
    }

    @Test
    @DisplayName("录入中的课程无需解锁")
    void shouldRejectUnlockWhenEditable() {
        when(courseMapper.selectById(1L)).thenReturn(course(0));

        assertThatThrownBy(() -> courseService.unlockScores(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无需解锁");
    }

    @Test
    @DisplayName("已提交的课程可以被管理员解锁")
    void shouldUnlockWhenSubmitted() {
        when(courseMapper.selectById(1L)).thenReturn(course(1));

        courseService.unlockScores(1L, 1L);

        verify(courseMapper).update(isNull(), any());
    }

    private Course course(Integer scoreStatus) {
        Course course = new Course();
        course.setId(1L);
        course.setCourseName("数据库原理");
        course.setScoreStatus(scoreStatus);
        return course;
    }
}
