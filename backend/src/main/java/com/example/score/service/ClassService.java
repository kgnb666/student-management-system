package com.example.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.score.entity.ClassInfo;
import com.example.score.entity.Student;
import com.example.score.exception.BusinessException;
import com.example.score.mapper.ClassInfoMapper;
import com.example.score.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassInfoMapper classInfoMapper;
    private final StudentMapper studentMapper;

    public List<ClassInfo> list() {
        return classInfoMapper.selectList(new LambdaQueryWrapper<ClassInfo>()
                .orderByDesc(ClassInfo::getGradeYear)
                .orderByAsc(ClassInfo::getClassName));
    }

    public ClassInfo getById(Long id) {
        ClassInfo classInfo = classInfoMapper.selectById(id);
        if (classInfo == null) {
            throw new BusinessException("班级不存在");
        }
        return classInfo;
    }

    public void create(ClassInfo classInfo) {
        checkClass(classInfo);
        classInfoMapper.insert(classInfo);
    }

    public void update(ClassInfo classInfo) {
        checkClass(classInfo);
        getById(classInfo.getId());
        classInfoMapper.updateById(classInfo);
    }

    public void delete(Long id) {
        getById(id);
        Long count = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getClassId, id));
        if (count > 0) {
            throw new BusinessException("该班级下还有学生，不能删除");
        }
        classInfoMapper.deleteById(id);
    }

    private void checkClass(ClassInfo classInfo) {
        if (classInfo.getClassName() == null || classInfo.getClassName().isBlank()) {
            throw new BusinessException("班级名称不能为空");
        }
        if (classInfo.getGradeYear() == null) {
            throw new BusinessException("请输入年级");
        }
    }
}
