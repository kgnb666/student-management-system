package com.example.score.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.score.entity.ScoreChangeLog;
import com.example.score.vo.ScoreChangeLogVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScoreChangeLogMapper extends BaseMapper<ScoreChangeLog> {

    List<ScoreChangeLogVO> selectLogList(@Param("courseId") Long courseId,
                                         @Param("studentId") Long studentId);

    IPage<ScoreChangeLogVO> selectLogListPage(IPage<ScoreChangeLogVO> page,
                                              @Param("courseId") Long courseId,
                                              @Param("studentId") Long studentId);
}
