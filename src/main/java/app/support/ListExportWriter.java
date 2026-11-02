package app.support;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ListExportWriter {

    private ListExportWriter() {
    }

    public static byte[] toCsv(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(escapeCsv(row.get(i)));
            }
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] toExcel(List<List<String>> rows, String sheetName) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            for (int r = 0; r < rows.size(); r++) {
                Row excelRow = sheet.createRow(r);
                List<String> cells = rows.get(r);
                for (int c = 0; c < cells.size(); c++) {
                    excelRow.createCell(c).setCellValue(cells.get(c) == null ? "" : cells.get(c));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
