package com.flowcontrol.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Excel 工具类
 * 使用 Apache POI 实现 Excel 导入/导出，支持大数据量流式写入
 */
@Slf4j
public class ExcelUtil {

    private static final String EXCEL2007_EXTENSION = ".xlsx";
    private static final String EXCEL2003_EXTENSION = ".xls";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 导出相关 ====================

    /**
     * 导出 Excel（使用流式写入，适合大数据量，最多支持100万行）
     *
     * @param fileName   文件名（不含扩展名）
     * @param sheetName  工作表名
     * @param headers    表头数组
     * @param dataList   数据列表（每行数据为一个String数组）
     * @param response   HTTP响应对象
     */
    public static void exportExcel(String fileName, String sheetName,
                                    String[] headers, List<String[]> dataList,
                                    HttpServletResponse response) {
        SXSSFWorkbook workbook = null;
        try {
            // 使用 SXSSFWorkbook 流式写入，内存中最多保留100行
            workbook = new SXSSFWorkbook(100);
            Sheet sheet = workbook.createSheet(sheetName);

            // 设置列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            // 1. 写入表头
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 2. 写入数据
            CellStyle dataStyle = createDataStyle(workbook);
            for (int rowIdx = 0; rowIdx < dataList.size(); rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                String[] rowData = dataList.get(rowIdx);
                for (int colIdx = 0; colIdx < rowData.length; colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    cell.setCellValue(rowData[colIdx] != null ? rowData[colIdx] : "");
                    cell.setCellStyle(dataStyle);
                }
            }

            // 3. 输出到响应
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String outputFileName = URLEncoder.encode(fileName + "_" + timestamp, "UTF-8")
                    + EXCEL2007_EXTENSION;

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + outputFileName + "\"");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.setCharacterEncoding("utf-8");

            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("Excel导出失败", e);
            throw new RuntimeException("Excel导出失败: " + e.getMessage());
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.warn("关闭workbook失败", e);
                }
                // 清理临时文件
                workbook.dispose();
            }
        }
    }

    /**
     * 创建表头样式（蓝色背景白色粗体）
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建数据行样式（白色背景黑色文字）
     */
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    // ==================== 导入相关 ====================

    /**
     * 从 MultipartFile 导入 Excel
     *
     * @param file       上传的文件
     * @param headerRowIndex 表头所在行（从0开始，默认0）
     * @param consumer   数据处理器，每解析一行调用一次
     */
    public static void importExcel(MultipartFile file, int headerRowIndex,
                                    Consumer<String[]> consumer) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(EXCEL2007_EXTENSION) && !fileName.endsWith(EXCEL2003_EXTENSION))) {
            throw new RuntimeException("仅支持 .xlsx 或 .xls 格式的Excel文件");
        }

        // 文件大小检查（10MB）
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("导入文件大小不能超过10MB");
        }

        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            // 跳过表头，读取数据行
            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();

            for (int rowNum = firstRowNum + headerRowIndex + 1; rowNum <= lastRowNum; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;

                // 读取该行所有单元格
                int lastCellNum = row.getLastCellNum();
                List<String> cellList = new ArrayList<>();
                boolean isEmptyRow = true;

                for (int cellNum = 0; cellNum < lastCellNum; cellNum++) {
                    Cell cell = row.getCell(cellNum);
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue != null && !cellValue.trim().isEmpty()) {
                        isEmptyRow = false;
                    }
                    cellList.add(cellValue);
                }

                // 跳过空行
                if (!isEmptyRow) {
                    consumer.accept(cellList.toArray(new String[0]));
                }
            }

            workbook.close();
        } catch (IOException e) {
            log.error("Excel导入失败", e);
            throw new RuntimeException("Excel导入失败: " + e.getMessage());
        }
    }

    /**
     * 获取单元格值并转为字符串
     */
    public static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().format(DATE_FORMATTER);
                }
                double numVal = cell.getNumericCellValue();
                // 避免科学计数法：如果是小数则保留2位
                if (numVal == Math.floor(numVal)) {
                    return String.valueOf((long) numVal);
                }
                return String.format("%.2f", numVal);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 生成导入模板 Excel
     */
    public static void generateTemplate(String fileName, String[] headers,
                                         HttpServletResponse response) {
        List<String[]> dataList = new ArrayList<>();
        // 空行，供用户参照填写
        for (int i = 0; i < 5; i++) {
            String[] emptyRow = new String[headers.length];
            for (int j = 0; j < emptyRow.length; j++) {
                emptyRow[j] = "";
            }
            dataList.add(emptyRow);
        }
        exportExcel(fileName, "导入模板", headers, dataList, response);
    }

    /**
     * LocalDateTime 格式化
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DATE_FORMATTER);
    }
}
