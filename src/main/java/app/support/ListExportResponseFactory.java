package app.support;

import app.config.AppListExportProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ListExportResponseFactory {

    private final AppListExportProperties exportProperties;
    private final MessageSource messageSource;

    public record ExportDecision<T>(boolean limited, String redirectPath, List<T> rows) {
    }

    public <T> ExportDecision<T> prepare(
            long matchingCount,
            String redirectPath,
            List<T> rowsWhenAllowed) {
        if (matchingCount > exportProperties.getMaxRows()) {
            return new ExportDecision<>(true, redirectPath, List.of());
        }
        return new ExportDecision<>(false, redirectPath, rowsWhenAllowed);
    }

    public String redirectWhenLimited(long count, String redirectPath, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(ListExportFlashAttributes.LIMIT_EXCEEDED, true);
        redirectAttributes.addFlashAttribute(ListExportFlashAttributes.ROW_COUNT, count);
        String message = messageSource.getMessage(
                "list.export.tooMany",
                new Object[]{count, exportProperties.getMaxRows()},
                LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute(ListExportFlashAttributes.LIMIT_MESSAGE, message);
        return "redirect:" + redirectPath;
    }

    public ResponseEntity<byte[]> csvResponse(String filename, List<List<String>> header, List<List<String>> dataRows)
            throws IOException {
        List<List<String>> all = new ArrayList<>(header);
        all.addAll(dataRows);
        byte[] body = ListExportWriter.toCsv(all);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    public ResponseEntity<byte[]> excelResponse(String filename, String sheetName, List<List<String>> header,
                                                List<List<String>> dataRows) throws IOException {
        List<List<String>> all = new ArrayList<>(header);
        all.addAll(dataRows);
        byte[] body = ListExportWriter.toExcel(all, sheetName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
