package com.example.score.mapper;

import com.example.score.vo.ScoreAggregateVO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL 聚合统计的集成测试，使用真实 MySQL，全部改动在事务中回滚。
 * MySQL 不可用时自动跳过，不影响纯单元测试。
 */
@SpringBootTest
@Transactional
class ScoreMapperAggregateTest {

    @Autowired
    private ScoreMapper scoreMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @BeforeEach
    void requireDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "MySQL 不可用，跳过统计集成测试：" + e.getMessage());
        }
    }

    @Test
    @DisplayName("SQL 聚合结果与按明细计算一致")
    void shouldMatchManualCalculation() {
        List<BigDecimal> scores = jdbcTemplate.queryForList(
                "SELECT final_score FROM score WHERE course_id = ? AND final_score IS NOT NULL",
                BigDecimal.class, 1L);
        assertThat(scores).isNotEmpty();

        ScoreAggregateVO aggregate = scoreMapper.selectScoreAggregate(1L, null, null, null);

        assertThat(aggregate.getTotal()).isEqualTo(scores.size());
        assertThat(aggregate.getAverage()).isEqualByComparingTo(round(
                scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0)));
        assertThat(aggregate.getMax()).isEqualByComparingTo(
                scores.stream().max(Comparator.naturalOrder()).orElseThrow());
        assertThat(aggregate.getMin()).isEqualByComparingTo(
                scores.stream().min(Comparator.naturalOrder()).orElseThrow());
        assertThat(aggregate.getPassCount())
                .isEqualTo(scores.stream().filter(score -> score.doubleValue() >= 60).count());
        assertThat(aggregate.getExcellentCount())
                .isEqualTo(scores.stream().filter(score -> score.doubleValue() >= 90).count());
    }

    @Test
    @DisplayName("五档分布边界 59/60/69/70/79/80/89/90 归类正确")
    void shouldClassifyBucketBoundaries() {
        long[][] cases = {
                {59, 0}, {60, 1}, {69, 1}, {70, 2},
                {79, 2}, {80, 3}, {89, 3}, {90, 4}
        };

        for (long[] testCase : cases) {
            jdbcTemplate.update(
                    "UPDATE score SET final_score = ? WHERE course_id = 1 AND student_id = 1",
                    testCase[0]);
            // JdbcTemplate 的更新不经过 MyBatis，需要手动清理会话级缓存，否则会命中上一次的聚合结果
            sqlSessionTemplate.clearCache();

            ScoreAggregateVO aggregate = scoreMapper.selectScoreAggregate(1L, null, 1L, null);

            assertThat(aggregate.getTotal()).as("total for %s", testCase[0]).isEqualTo(1L);
            assertThat(bucketValues(aggregate))
                    .as("final_score=%s 应落入第 %s 档", testCase[0], testCase[1])
                    .containsOnlyOnce(1L);
            assertThat(bucketValues(aggregate).get((int) testCase[1]))
                    .as("final_score=%s 应落入第 %s 档", testCase[0], testCase[1])
                    .isEqualTo(1L);
        }
    }

    private List<Long> bucketValues(ScoreAggregateVO aggregate) {
        return List.of(aggregate.getBucket0(), aggregate.getBucket1(), aggregate.getBucket2(),
                aggregate.getBucket3(), aggregate.getBucket4());
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
