package app.service;

import app.model.enums.AttachmentFileType;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UploadContentValidator {

    public record ValidatedUpload(AttachmentFileType fileType, String extension) {}

    private static final Tika TIKA = new Tika();

    private final Set<AttachmentFileType> allowedTypes;

    public UploadContentValidator(
            @Value("${app.upload.allowed-types:jpeg,png,pdf}") String allowedTypesProperty) {
        this.allowedTypes = Arrays.stream(allowedTypesProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .map(this::parseAllowedToken)
                .collect(Collectors.toUnmodifiableSet());
        if (this.allowedTypes.isEmpty()) {
            throw new IllegalStateException("app.upload.allowed-types must list at least one type");
        }
    }

    public ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        AttachmentFileType detectedType = detectType(file);
        if (!allowedTypes.contains(detectedType)) {
            throw new IllegalArgumentException(
                    "File type not allowed. Only JPEG, PNG, and PDF files are accepted.");
        }

        String extensionFromContent = extensionFor(detectedType);
        String extensionFromName = extensionFromFilename(file.getOriginalFilename());
        if (extensionFromName != null && !extensionFromName.equalsIgnoreCase(extensionFromContent)
                && !jpegExtensionsEquivalent(extensionFromName, extensionFromContent, detectedType)) {
            throw new IllegalArgumentException("File content does not match the file extension.");
        }

        return new ValidatedUpload(detectedType, extensionFromContent);
    }

    private AttachmentFileType parseAllowedToken(String token) {
        return switch (token) {
            case "jpeg", "jpg" -> AttachmentFileType.JPG;
            case "png" -> AttachmentFileType.PNG;
            case "pdf" -> AttachmentFileType.PDF;
            default -> throw new IllegalStateException("Unsupported upload type in app.upload.allowed-types: " + token);
        };
    }

    private AttachmentFileType detectType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            String mediaTypeName = TIKA.detect(in, file.getOriginalFilename());
            MediaType mediaType = MediaType.parse(mediaTypeName);
            String base = mediaType.getBaseType().toString();
            return switch (base) {
                case "image/jpeg" -> AttachmentFileType.JPG;
                case "image/png" -> AttachmentFileType.PNG;
                case "application/pdf" -> AttachmentFileType.PDF;
                default -> AttachmentFileType.OTHER;
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read uploaded file.");
        }
    }

    private static String extensionFromFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return null;
        }
        String cleaned = StringUtils.cleanPath(originalFilename);
        int dot = cleaned.lastIndexOf('.');
        if (dot < 0 || dot == cleaned.length() - 1) {
            return null;
        }
        return cleaned.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static String extensionFor(AttachmentFileType type) {
        return switch (type) {
            case JPG -> ".jpg";
            case PNG -> ".png";
            case PDF -> ".pdf";
            default -> "";
        };
    }

    private static boolean jpegExtensionsEquivalent(
            String extensionFromName, String extensionFromContent, AttachmentFileType type) {
        if (type != AttachmentFileType.JPG) {
            return false;
        }
        Set<String> jpegExts = Set.of(".jpg", ".jpeg");
        return jpegExts.contains(extensionFromName.toLowerCase(Locale.ROOT))
                && jpegExts.contains(extensionFromContent.toLowerCase(Locale.ROOT));
    }
}
