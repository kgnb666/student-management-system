package com.example.score.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.score.entity.Score;
import com.example.score.entity.ScoreChangeLog;
import com.example.score.entity.User;
import com.example.score.mapper.ScoreChangeLogMapper;
import com.example.score.mapper.UserMapper;
import com.example.score.vo.PageResult;
import com.example.score.vo.ScoreChangeLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 成绩变更审计：主流程在同一事务内调用，主操作回滚时日志一并回滚。
 */
@Service
@RequiredArgsConstructor
public class ScoreChangeLogService {

    private final ScoreChangeLogMapper logMapper;
    private final UserMapper userMapper;

    /**
     * 记录单条成绩的新增、修改或删除。
     *
     * @param before 变更前记录，新增时为 null
     * @param after  变更后记录，删除时为 null
     */
    public void recordScoreChange(Score before, Score after, String action, Long operatorUserId, String remark) {
        ScoreChangeLog log = new ScoreChangeLog();
        log.setScoreId(after != null ? after.getId() : before.getId());
        log.setCourseId(after != null ? after.getCourseId() : before.getCourseId());
        log.setStudentId(after != null ? after.getStudentId() : before.getStudentId());
        log.setAction(action);
        log.setRemark(remark);
        fillOperator(log, operatorUserId);
        if (before != null) {
            log.setBeforeUsualScore(before.getUsualScore());
            log.setBeforeExamScore(before.getExamScore());
            log.setBeforeFinalScore(before.getFinalScore());
        }
        if (after != null) {
            log.setAfterUsualScore(after.getUsualScore());
            log.setAfterExamScore(after.getExamScore());
            log.setAfterFinalScore(after.getFinalScore());
        }
        logMapper.insert(log);
    }

    /**
     * 记录课程级动作（提交锁定 / 解锁）。
     */
    public void recordCourseAction(Long courseId, String action, Long operatorUserId, String remark) {
        ScoreChangeLog log = new ScoreChangeLog();
        log.setCourseId(courseId);
        log.setAction(action);
        log.setRemark(remark);
        fillOperator(log, operatorUserId);
        logMapper.insert(log);
    }

    public List<ScoreChangeLogVO> list(Long courseId, Long studentId) {
        return logMapper.selectLogList(courseId, studentId);
    }

    public PageResult<ScoreChangeLogVO> listPaged(Long courseId, Long studentId, Long page, Long size) {
        Page<ScoreChangeLogVO> pageParam = new Page<>(
                PageResult.normalizeCurrent(page), PageResult.normalizeSize(size));
        return PageResult.of(logMapper.selectLogListPage(pageParam, courseId, studentId));
    }

    private void fillOperator(ScoreChangeLog log, Long operatorUserId) {
        log.setOperatorUserId(operatorUserId);
        if (operatorUserId == null) {
            return;
        }
        User operator = userMapper.selectById(operatorUserId);
        if (operator != null) {
            log.setOperatorName(operator.getRealName());
            log.setOperatorRole(operator.getRole());
        }
    }
}
