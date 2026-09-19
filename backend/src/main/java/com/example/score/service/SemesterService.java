package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.score.entity.Course;
import com.example.score.entity.Semester;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.CourseMapper;
import com.example.score.mapper.SemesterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterMapper semesterMapper;
    private final CourseMapper courseMapper;

    public List<Semester> list() {
        return semesterMapper.selectList(new LambdaQueryWrapper<Semester>()
                .orderByDesc(Semester::getStartDate));
    }

    public Semester getById(Long id) {
        Semester semester = semesterMapper.selectById(id);
        if (semester == null) {
            throw new BusinessException("学期不存在");
        }
        return semester;
    }

    public void create(Semester semester) {
        checkSemester(semester);
        semester.setIsCurrent(0);
        semesterMapper.insert(semester);
    }

    public void update(Semester semester) {
        checkSemester(semester);
        Semester oldSemester = getById(semester.getId());
        semester.setIsCurrent(oldSemester.getIsCurrent());
        semesterMapper.updateById(semester);
    }

    public void delete(Long id) {
        getById(id);
        Long count = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getSemesterId, id));
        if (count > 0) {
            throw new BusinessException("该学期下还有课程，不能删除");
        }
        semesterMapper.deleteById(id);
    }

    @Transactional
    public void setCurrent(Long id) {
        getById(id);
        semesterMapper.update(null, new LambdaUpdateWrapper<Semester>()
                .set(Semester::getIsCurrent, 0));
        semesterMapper.update(null, new LambdaUpdateWrapper<Semester>()
                .eq(Semester::getId, id)
                .set(Semester::getIsCurrent, 1));
    }

    private void checkSemester(Semester semester) {
        if (semester.getSemesterName() == null || semester.getSemesterName().isBlank()) {
            throw new BusinessException("学期名称不能为空");
        }
        if (semester.getStartDate() == null || semester.getEndDate() == null) {
            throw new BusinessException("请选择开始和结束日期");
        }
        if (semester.getEndDate().isBefore(semester.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
    }
}
