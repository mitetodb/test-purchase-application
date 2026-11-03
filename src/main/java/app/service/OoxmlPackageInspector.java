package app.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Inspects ZIP-based Office packages using {@code [Content_Types].xml} only (never filename).
 */
final class OoxmlPackageInspector {

    private static final String CONTENT_TYPES_PATH = "[Content_Types].xml";

    private static final String DOCX_MAIN =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
    private static final String XLSX_MAIN =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";

    private static final Pattern CONTENT_TYPE_ATTR =
            Pattern.compile("ContentType\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    enum ZipDocumentKind {
        NOT_ZIP,
        NOT_OOXML,
        DOCX,
        XLSX,
        MACRO_OR_UNSUPPORTED
    }

    private OoxmlPackageInspector() {}

    static boolean isZipArchive(byte[] header) {
        return header.length >= 4
                && header[0] == 'P'
                && header[1] == 'K'
                && header[2] == 3
                && header[3] == 4;
    }

    static ZipDocumentKind inspect(byte[] fileBytes) throws IOException {
        if (fileBytes.length < 4 || !isZipArchive(fileBytes)) {
            return ZipDocumentKind.NOT_ZIP;
        }
        try (InputStream in = new ByteArrayInputStream(fileBytes);
             ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (CONTENT_TYPES_PATH.equals(entry.getName())) {
                    String xml = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    return classifyContentTypesXml(xml);
                }
            }
        }
        return ZipDocumentKind.NOT_OOXML;
    }

    private static ZipDocumentKind classifyContentTypesXml(String xml) {
        var matcher = CONTENT_TYPE_ATTR.matcher(xml);
        boolean hasDocxMain = false;
        boolean hasXlsxMain = false;
        while (matcher.find()) {
            String type = matcher.group(1).toLowerCase(Locale.ROOT);
            if (type.contains("macroenabled")) {
                return ZipDocumentKind.MACRO_OR_UNSUPPORTED;
            }
            if (DOCX_MAIN.equalsIgnoreCase(type)) {
                hasDocxMain = true;
            }
            if (XLSX_MAIN.equalsIgnoreCase(type)) {
                hasXlsxMain = true;
            }
        }
        if (hasDocxMain && !hasXlsxMain) {
            return ZipDocumentKind.DOCX;
        }
        if (hasXlsxMain && !hasDocxMain) {
            return ZipDocumentKind.XLSX;
        }
        return ZipDocumentKind.MACRO_OR_UNSUPPORTED;
    }
}
