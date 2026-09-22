package com.example.score.service;

import com.example.score.dto.BatchScoreRequest;
import com.example.score.dto.ScoreItem;
import com.example.score.exception.BusinessException;
import com.example.score.vo.CourseStudentVO;
import com.example.score.vo.ScoreImportResultVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课程成绩单的 Excel 导入导出。
 *
 * <p>模板列顺序：学号、姓名、班级、平时成绩、期末成绩、总评成绩。
 * 导入时只读取学号与两项成绩，姓名与班级仅作展示。</p>
 */
@Service
@RequiredArgsConstructor
public class ScoreExcelService {

    private static final String[] TITLES = {"学号", "姓名", "班级", "平时成绩", "期末成绩", "总评成绩"};
    private static final int COLUMN_STUDENT_NO = 0;
    private static final int COLUMN_USUAL_SCORE = 3;
    private static final int COLUMN_EXAM_SCORE = 4;
    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(100);

    private final ScoreService scoreService;

    public byte[] exportCourseScores(Long courseId) {
        List<CourseStudentVO> students = scoreService.courseStudents(courseId);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("成绩单");
            Row header = sheet.createRow(0);
            for (int i = 0; i < TITLES.length; i++) {
                header.createCell(i).setCellValue(TITLES[i]);
                sheet.setColumnWidth(i, i == 2 ? 6000 : 3500);
            }

            int rowIndex = 1;
            for (CourseStudentVO student : students) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(student.getStudentNo());
                row.createCell(1).setCellValue(student.getStudentName());
                row.createCell(2).setCellValue(student.getClassName() == null ? "" : student.getClassName());
                writeScore(row, COLUMN_USUAL_SCORE, student.getUsualScore());
                writeScore(row, COLUMN_EXAM_SCORE, student.getExamScore());
                writeScore(row, 5, student.getFinalScore());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("导出成绩单失败");
        }
    }

    @Transactional
    public ScoreImportResultVO importCourseScores(Long courseId, MultipartFile file, Long operatorUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的 Excel 文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BusinessException("只支持 .xlsx 格式的成绩单");
        }
        // 已提交锁定的课程不允许导入
        scoreService.checkCourseEditable(courseId);

        Map<String, CourseStudentVO> roster = scoreService.courseStudents(courseId).stream()
                .collect(Collectors.toMap(CourseStudentVO::getStudentNo, Function.identity(), (a, b) -> a));

        List<String> errors = new ArrayList<>();
        Map<Long, ScoreItem> items = new LinkedHashMap<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String studentNo = readText(row.getCell(COLUMN_STUDENT_NO));
                if (studentNo.isBlank()) {
                    continue;
                }
                CourseStudentVO student = roster.get(studentNo);
                if (student == null) {
                    errors.add("第 " + (rowIndex + 1) + " 行：学号 " + studentNo + " 不在该课程名单中");
                    continue;
                }

                BigDecimal usualScore;
                BigDecimal examScore;
                try {
                    usualScore = readScore(row.getCell(COLUMN_USUAL_SCORE));
                    examScore = readScore(row.getCell(COLUMN_EXAM_SCORE));
                } catch (IllegalArgumentException e) {
                    errors.add("第 " + (rowIndex + 1) + " 行：" + e.getMessage());
                    continue;
                }
                if (usualScore == null && examScore == null) {
                    // 两项都空视为未录入，跳过而不是清空已有成绩
                    continue;
                }

                ScoreItem item = new ScoreItem();
                item.setStudentId(student.getStudentId());
                item.setUsualScore(usualScore);
                item.setExamScore(examScore);
                items.put(student.getStudentId(), item);
            }
        } catch (IOException e) {
            throw new BusinessException("Excel 文件解析失败，请确认文件未损坏");
        }

        if (!items.isEmpty()) {
            BatchScoreRequest request = new BatchScoreRequest();
            request.setCourseId(courseId);
            request.setScores(new ArrayList<>(items.values()));
            scoreService.saveBatch(request, operatorUserId);
        }

        return new ScoreImportResultVO(items.size(), errors.size(), errors);
    }

    private void writeScore(Row row, int column, BigDecimal score) {
        if (score == null) {
            return;
        }
        row.createCell(column).setCellValue(score.doubleValue());
    }

    private String readText(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (value == Math.floor(value)) {
                return String.valueOf((long) value);
            }
            return String.valueOf(value);
        }
        return cell.toString().trim();
    }

    private BigDecimal readScore(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().isBlank()) {
            return null;
        }

        BigDecimal score;
        try {
            score = cell.getCellType() == CellType.NUMERIC
                    ? BigDecimal.valueOf(cell.getNumericCellValue())
                    : new BigDecimal(cell.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("成绩必须是数字");
        }
        score = score.setScale(1, java.math.RoundingMode.HALF_UP);
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new IllegalArgumentException("成绩必须在 0 到 100 之间");
        }
        return score;
    }
}
