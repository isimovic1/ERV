package hr.algebra.workforce.export;

import hr.algebra.workforce.dto.MonthlyReport;
import hr.algebra.workforce.dto.MonthlyReportRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

@Component
public class ExcelReportExporter {

    private static final List<String> HEADERS = List.of("Zaposlenik", "Radnih dana", "Sati", "Prekovremeni",
            "Dana godišnjeg", "Dana bolovanja");

    public byte[] export(MonthlyReport report) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Evidencija " + report.month());
            CellStyle boldStyle = boldStyle(workbook);
            writeHeader(sheet, boldStyle);
            int rowIndex = 1;
            for (MonthlyReportRow row : report.rows()) {
                writeRow(sheet.createRow(rowIndex++), row);
            }
            writeTotals(sheet.createRow(rowIndex), report, boldStyle);
            autoSize(sheet);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Izvoz u Excel nije uspio", exception);
        }
    }

    private CellStyle boldStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeHeader(Sheet sheet, CellStyle style) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS.get(i));
            cell.setCellStyle(style);
        }
    }

    private void writeRow(Row row, MonthlyReportRow source) {
        row.createCell(0).setCellValue(source.employeeName());
        row.createCell(1).setCellValue(source.workedDays());
        row.createCell(2).setCellValue(source.workedHours().doubleValue());
        row.createCell(3).setCellValue(source.overtimeHours().doubleValue());
        row.createCell(4).setCellValue(source.vacationDays());
        row.createCell(5).setCellValue(source.sickDays());
    }

    private void writeTotals(Row row, MonthlyReport report, CellStyle style) {
        Cell label = row.createCell(0);
        label.setCellValue("Ukupno");
        label.setCellStyle(style);
        row.createCell(2).setCellValue(toDouble(report.totalHours()));
        row.createCell(3).setCellValue(toDouble(report.totalOvertimeHours()));
        row.createCell(4).setCellValue(report.totalVacationDays());
        row.createCell(5).setCellValue(report.totalSickDays());
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    private void autoSize(Sheet sheet) {
        for (int i = 0; i < HEADERS.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
