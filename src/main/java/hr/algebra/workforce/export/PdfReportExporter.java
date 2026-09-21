package hr.algebra.workforce.export;

import hr.algebra.workforce.dto.MonthlyReport;
import hr.algebra.workforce.dto.MonthlyReportRow;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

@Component
public class PdfReportExporter {

    private static final List<String> HEADERS = List.of("Zaposlenik", "Radnih dana", "Sati", "Prekovremeni",
            "Dana godišnjeg", "Dana bolovanja");

    public byte[] export(MonthlyReport report) {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(title("Evidencija radnog vremena za " + report.month()));
            document.add(buildTable(report));
            document.close();
            return output.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Izvoz u PDF nije uspio", exception);
        }
    }

    private Paragraph title(String text) {
        Paragraph paragraph = new Paragraph(text, font(14, Font.BOLD));
        paragraph.setSpacingAfter(12);
        return paragraph;
    }

    private PdfPTable buildTable(MonthlyReport report) throws DocumentException {
        PdfPTable table = new PdfPTable(HEADERS.size());
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 2, 2, 2, 2, 2});
        HEADERS.forEach(header -> table.addCell(headerCell(header)));
        for (MonthlyReportRow row : report.rows()) {
            table.addCell(cell(row.employeeName(), Element.ALIGN_LEFT));
            table.addCell(cell(String.valueOf(row.workedDays()), Element.ALIGN_RIGHT));
            table.addCell(cell(format(row.workedHours()), Element.ALIGN_RIGHT));
            table.addCell(cell(format(row.overtimeHours()), Element.ALIGN_RIGHT));
            table.addCell(cell(String.valueOf(row.vacationDays()), Element.ALIGN_RIGHT));
            table.addCell(cell(String.valueOf(row.sickDays()), Element.ALIGN_RIGHT));
        }
        table.addCell(headerCell("Ukupno"));
        table.addCell(headerCell(""));
        table.addCell(headerCell(format(report.totalHours())));
        table.addCell(headerCell(format(report.totalOvertimeHours())));
        table.addCell(headerCell(String.valueOf(report.totalVacationDays())));
        table.addCell(headerCell(String.valueOf(report.totalSickDays())));
        return table;
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(10, Font.BOLD)));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell cell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(10, Font.NORMAL)));
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private String format(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private Font font(int size, int style) {
        try {
            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.NOT_EMBEDDED);
            return new Font(baseFont, size, style);
        } catch (Exception exception) {
            throw new IllegalStateException("Font za PDF nije dostupan", exception);
        }
    }
}
