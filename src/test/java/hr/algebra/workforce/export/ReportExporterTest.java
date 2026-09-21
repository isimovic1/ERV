package hr.algebra.workforce.export;

import hr.algebra.workforce.dto.MonthlyReport;
import hr.algebra.workforce.dto.MonthlyReportRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportExporterTest {

    private static final MonthlyReport REPORT = MonthlyReport.of(YearMonth.of(2026, 9), List.of(
            new MonthlyReportRow(1L, "Ivan Šimović", new BigDecimal("160.00"), new BigDecimal("4.50"), 20, 2, 1),
            new MonthlyReportRow(2L, "Đurđica Šešelj-Čić", new BigDecimal("120.00"), BigDecimal.ZERO, 15, 0, 5)));

    private final ExcelReportExporter excelExporter = new ExcelReportExporter();
    private final PdfReportExporter pdfExporter = new PdfReportExporter();

    @Test
    void excelContainsCroatianNamesAndTotals() throws IOException {
        byte[] content = excelExporter.export(REPORT);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Zaposlenik");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Ivan Šimović");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Đurđica Šešelj-Čić");
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(160.0);

            Row totals = sheet.getRow(3);
            assertThat(totals.getCell(0).getStringCellValue()).isEqualTo("Ukupno");
            assertThat(totals.getCell(2).getNumericCellValue()).isEqualTo(280.0);
            assertThat(totals.getCell(5).getNumericCellValue()).isEqualTo(6.0);
        }
    }

    @Test
    void pdfIsProducedWithCentralEuropeanEncoding() {
        byte[] content = pdfExporter.export(REPORT);
        String raw = new String(content, Charset.forName("ISO-8859-1"));

        assertThat(new String(content, 0, 5, Charset.forName("ISO-8859-1"))).isEqualTo("%PDF-");
        assertThat(content.length).isGreaterThan(500);
        assertThat(raw).contains("/Differences");
        assertThat(raw).contains("Scaron");
    }
}
