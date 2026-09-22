package com.example.score.service;

import com.example.score.dto.BatchScoreRequest;
import com.example.score.exception.BusinessException;
import com.example.score.vo.CourseStudentVO;
import com.example.score.vo.ScoreImportResultVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成绩单 Excel 导出与导入的单元测试（内存中构造 xlsx，不依赖 MySQL）。
 */
@ExtendWith(MockitoExtension.class)
class ScoreExcelServiceTest {

    @Mock
    private ScoreService scoreService;

    @InjectMocks
    private ScoreExcelService scoreExcelService;

    @Test
    @DisplayName("导出成绩单包含表头与课程学生成绩，未录入成绩的行留空")
    void shouldExportScores() throws Exception {
        mockRoster();

        byte[] content = scoreExcelService.exportCourseScores(1L);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("学号");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("2023001");
            assertThat(sheet.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(85.0);
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("2023002");
            assertThat(sheet.getRow(2).getCell(3)).isNull();
        }
    }

    @Test
    @DisplayName("导入时逐行校验：名单外的学号与越界成绩被拒绝，合法行正常保存")
    void shouldImportWithRowErrors() throws Exception {
        mockRoster();
        byte[] content = workbook(
                new String[]{"学号", "姓名", "班级", "平时成绩", "期末成绩", "总评成绩"},
                new String[]{"2023001", "王小明", "软件工程2301班", "80", "90", ""},
                new String[]{"9999999", "查无此人", "未知班级", "80", "90", ""},
                new String[]{"2023002", "李雨桐", "软件工程2301班", "120", "90", ""},
                new String[]{"", "", "", "", "", ""}
        );

        ScoreImportResultVO result = scoreExcelService.importCourseScores(1L, multipart(content), 1L);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failCount()).isEqualTo(2);
        assertThat(result.errors()).anySatisfy(error -> assertThat(error).contains("不在该课程名单中"));
        assertThat(result.errors())
                .anySatisfy(error -> assertThat(error).contains("成绩必须在 0 到 100 之间"));

        ArgumentCaptor<BatchScoreRequest> captor = ArgumentCaptor.forClass(BatchScoreRequest.class);
        verify(scoreService).saveBatch(captor.capture(), eq(1L));
        assertThat(captor.getValue().getCourseId()).isEqualTo(1L);
        assertThat(captor.getValue().getScores()).hasSize(1);
        assertThat(captor.getValue().getScores().get(0).getStudentId()).isEqualTo(1L);
        assertThat(captor.getValue().getScores().get(0).getUsualScore()).isEqualByComparingTo("80.0");
        assertThat(captor.getValue().getScores().get(0).getExamScore()).isEqualByComparingTo("90.0");
    }

    @Test
    @DisplayName("非 xlsx 文件被拒绝")
    void shouldRejectNonExcelFile() {
        MockMultipartFile file = new MockMultipartFile("file", "scores.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> scoreExcelService.importCourseScores(1L, file, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只支持 .xlsx");
    }

    private void mockRoster() {
        when(scoreService.courseStudents(1L)).thenReturn(List.of(
                student(1L, "2023001", "王小明", "软件工程2301班", "85.0", "88.0"),
                student(2L, "2023002", "李雨桐", "软件工程2301班", null, null)
        ));
    }

    private MockMultipartFile multipart(byte[] content) {
        return new MockMultipartFile(
                "file", "scores.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }

    private byte[] workbook(String[] header, String[]... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("成绩单");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < header.length; i++) {
                headerRow.createCell(i).setCellValue(header[i]);
            }
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < rows[i].length; j++) {
                    row.createCell(j).setCellValue(rows[i][j]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CourseStudentVO student(Long id, String studentNo, String name, String className,
                                    String usualScore, String examScore) {
        CourseStudentVO vo = new CourseStudentVO();
        vo.setStudentId(id);
        vo.setStudentNo(studentNo);
        vo.setStudentName(name);
        vo.setClassName(className);
        vo.setUsualScore(usualScore == null ? null : new BigDecimal(usualScore));
        vo.setExamScore(examScore == null ? null : new BigDecimal(examScore));
        return vo;
    }
}
