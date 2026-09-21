package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.dto.MonthlyReport;
import hr.algebra.workforce.export.ExcelReportExporter;
import hr.algebra.workforce.export.PdfReportExporter;
import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private static final String EXCEL_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportService reportService;
    private final ExcelReportExporter excelReportExporter;
    private final PdfReportExporter pdfReportExporter;

    @GetMapping
    public String report(@AuthenticationPrincipal AppUserDetails principal,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                         Model model) {
        YearMonth selected = month == null ? YearMonth.now() : month;
        model.addAttribute("report", reportService.monthlyReport(principal.getId(), principal.getRole(), selected));
        model.addAttribute("month", selected);
        model.addAttribute("previousMonth", selected.minusMonths(1));
        model.addAttribute("nextMonth", selected.plusMonths(1));
        return "reports";
    }

    @GetMapping("/excel")
    public ResponseEntity<Resource> excel(@AuthenticationPrincipal AppUserDetails principal,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth selected = month == null ? YearMonth.now() : month;
        MonthlyReport report = reportService.monthlyReport(principal.getId(), principal.getRole(), selected);
        return download(excelReportExporter.export(report), "evidencija-" + selected + ".xlsx",
                MediaType.parseMediaType(EXCEL_TYPE));
    }

    @GetMapping("/pdf")
    public ResponseEntity<Resource> pdf(@AuthenticationPrincipal AppUserDetails principal,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        YearMonth selected = month == null ? YearMonth.now() : month;
        MonthlyReport report = reportService.monthlyReport(principal.getId(), principal.getRole(), selected);
        return download(pdfReportExporter.export(report), "evidencija-" + selected + ".pdf",
                MediaType.APPLICATION_PDF);
    }

    private ResponseEntity<Resource> download(byte[] content, String fileName, MediaType mediaType) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new ByteArrayResource(content));
    }
}
