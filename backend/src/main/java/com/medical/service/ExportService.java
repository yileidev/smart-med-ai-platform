package com.medical.service;

import com.medical.entity.DiagnosisHistory;
import com.medical.entity.OperationLog;
import com.medical.entity.TriageRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出诊断历史Excel
     */
    public byte[] exportDiagnosisHistoryToExcel(List<DiagnosisHistory> histories) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("诊断历史");
            
            // 创建标题行样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "患者ID", "医生姓名", "诊断结果", "治疗方案", 
                              "优先级", "诊断时间", "AI诊断", "置信度"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            int rowNum = 1;
            for (DiagnosisHistory history : histories) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(history.getId());
                row.createCell(1).setCellValue(history.getPatientId());
                row.createCell(2).setCellValue(history.getDoctorName());
                row.createCell(3).setCellValue(history.getDiagnosis());
                row.createCell(4).setCellValue(history.getTreatment());
                row.createCell(5).setCellValue(history.getPriority());
                row.createCell(6).setCellValue(
                    history.getDiagnosisTime().format(DATE_FORMATTER));
                row.createCell(7).setCellValue(history.getAiDiagnosis());
                row.createCell(8).setCellValue(
                    history.getConfidenceScore() != null ? history.getConfidenceScore() : 0.0);
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 导出操作日志Excel
     */
    public byte[] exportOperationLogsToExcel(List<OperationLog> logs) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("操作日志");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "用户名", "操作类型", "模块", "描述", 
                              "状态", "操作时间", "执行时长(ms)"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int rowNum = 1;
            for (OperationLog log : logs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getUsername());
                row.createCell(2).setCellValue(log.getOperationType());
                row.createCell(3).setCellValue(log.getModule());
                row.createCell(4).setCellValue(log.getDescription());
                row.createCell(5).setCellValue(log.getStatus());
                row.createCell(6).setCellValue(
                    log.getOperationTime().format(DATE_FORMATTER));
                row.createCell(7).setCellValue(
                    log.getDuration() != null ? log.getDuration() : 0L);
            }
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 导出分诊记录Excel
     */
    public byte[] exportTriageRecordsToExcel(List<TriageRecord> records) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("分诊记录");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "患者姓名", "主诉", "分诊等级", "科室", 
                              "到院时间", "AI诊断", "置信度"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int rowNum = 1;
            for (TriageRecord record : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.getId());
                row.createCell(1).setCellValue(
                    record.getPatient() != null ? record.getPatient().getPatientName() : "");
                row.createCell(2).setCellValue(record.getChiefComplaint());
                row.createCell(3).setCellValue(record.getTriageLevel());
                row.createCell(4).setCellValue(record.getAssignedDepartment());
                row.createCell(5).setCellValue(
                    record.getArrivalTime().format(DATE_FORMATTER));
                row.createCell(6).setCellValue(record.getAiDiagnosis());
                row.createCell(7).setCellValue(
                    record.getTriageScore() != null ? record.getTriageScore().doubleValue() : 0.0);
            }
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 创建标题行样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
