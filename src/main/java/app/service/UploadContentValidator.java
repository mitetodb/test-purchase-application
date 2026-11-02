package app.service;

import app.exception.UploadRejectedException;
import app.exception.UploadRejectedException.UploadTarget;
import app.model.enums.AttachmentFileType;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
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

    private static final Set<String> BLOCKED_MEDIA_TYPES = Set.of(
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-word.document.macroenabled.12",
            "application/vnd.ms-excel.sheet.macroenabled.12"
    );

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".doc", ".xls", ".docm", ".xlsm"
    );

    private final Set<AttachmentFileType> attachmentAllowedTypes;
    private final Set<AttachmentFileType> avatarAllowedTypes;
    private final long maxFileSizeBytes;

    public UploadContentValidator(
            @Value("${app.upload.attachment.allowed-types:jpeg,png,pdf,docx,xlsx}") String attachmentAllowed,
            @Value("${app.upload.avatar.allowed-types:jpeg,png}") String avatarAllowed,
            @Value("${spring.servlet.multipart.max-file-size:10MB}") DataSize maxFileSize) {
        this.attachmentAllowedTypes = parseAllowedTypes(attachmentAllowed);
        this.avatarAllowedTypes = parseAllowedTypes(avatarAllowed);
        this.maxFileSizeBytes = maxFileSize.toBytes();
    }

    public ValidatedUpload validateForAttachment(MultipartFile file) {
        return validate(file, attachmentAllowedTypes, UploadTarget.ATTACHMENT);
    }

    public ValidatedUpload validateForAvatar(MultipartFile file) {
        return validate(file, avatarAllowedTypes, UploadTarget.AVATAR);
    }

    /** @deprecated use {@link #validateForAttachment} */
    @Deprecated
    public ValidatedUpload validate(MultipartFile file) {
        return validateForAttachment(file);
    }

    private ValidatedUpload validate(
            MultipartFile file, Set<AttachmentFileType> allowedTypes, UploadTarget target) {
        if (file == null || file.isEmpty()) {
            throw new UploadRejectedException(UploadRejectedException.Reason.EMPTY, target);
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new UploadRejectedException(UploadRejectedException.Reason.TOO_LARGE, target);
        }

        rejectBlockedFilenameExtension(file.getOriginalFilename(), target);

        DetectedType detected = detectType(file, target);
        if (detected.blockedLegacyOrMacro()) {
            throw new UploadRejectedException(UploadRejectedException.Reason.TYPE_NOT_ALLOWED, target);
        }
        if (!allowedTypes.contains(detected.fileType())) {
            throw new UploadRejectedException(UploadRejectedException.Reason.TYPE_NOT_ALLOWED, target);
        }

        String extensionFromContent = extensionFor(detected.fileType());
        String extensionFromName = extensionFromFilename(file.getOriginalFilename());
        if (extensionFromName != null && !extensionsCompatible(extensionFromName, extensionFromContent, detected.fileType())) {
            throw new UploadRejectedException(UploadRejectedException.Reason.EXTENSION_MISMATCH, target);
        }

        return new ValidatedUpload(detected.fileType(), extensionFromContent);
    }

    private static void rejectBlockedFilenameExtension(String originalFilename, UploadTarget target) {
        String ext = extensionFromFilename(originalFilename);
        if (ext != null && BLOCKED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new UploadRejectedException(UploadRejectedException.Reason.TYPE_NOT_ALLOWED, target);
        }
    }

    private record DetectedType(AttachmentFileType fileType, boolean blockedLegacyOrMacro) {}

    private DetectedType detectType(MultipartFile file, UploadTarget target) {
        try (InputStream in = file.getInputStream()) {
            String mediaTypeName = TIKA.detect(in, file.getOriginalFilename());
            MediaType mediaType = MediaType.parse(mediaTypeName);
            String base = mediaType.getBaseType().toString().toLowerCase(Locale.ROOT);
            if (BLOCKED_MEDIA_TYPES.contains(base)) {
                return new DetectedType(AttachmentFileType.OTHER, true);
            }
            AttachmentFileType type = switch (base) {
                case "image/jpeg" -> AttachmentFileType.JPG;
                case "image/png" -> AttachmentFileType.PNG;
                case "application/pdf" -> AttachmentFileType.PDF;
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> AttachmentFileType.DOC;
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> AttachmentFileType.XLS;
                default -> AttachmentFileType.OTHER;
            };
            return new DetectedType(type, false);
        } catch (IOException e) {
            throw new UploadRejectedException(UploadRejectedException.Reason.READ_ERROR, target);
        }
    }

    private static Set<AttachmentFileType> parseAllowedTypes(String property) {
        Set<AttachmentFileType> types = Arrays.stream(property.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .map(UploadContentValidator::parseAllowedToken)
                .collect(Collectors.toUnmodifiableSet());
        if (types.isEmpty()) {
            throw new IllegalStateException("Upload allowlist must list at least one type");
        }
        return types;
    }

    private static AttachmentFileType parseAllowedToken(String token) {
        return switch (token) {
            case "jpeg", "jpg" -> AttachmentFileType.JPG;
            case "png" -> AttachmentFileType.PNG;
            case "pdf" -> AttachmentFileType.PDF;
            case "docx" -> AttachmentFileType.DOC;
            case "xlsx" -> AttachmentFileType.XLS;
            default -> throw new IllegalStateException("Unsupported upload type token: " + token);
        };
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
            case DOC -> ".docx";
            case XLS -> ".xlsx";
            default -> "";
        };
    }

    private static boolean extensionsCompatible(
            String extensionFromName, String extensionFromContent, AttachmentFileType type) {
        if (extensionFromName.equalsIgnoreCase(extensionFromContent)) {
            return true;
        }
        if (type == AttachmentFileType.JPG) {
            Set<String> jpegExts = Set.of(".jpg", ".jpeg");
            return jpegExts.contains(extensionFromName.toLowerCase(Locale.ROOT))
                    && jpegExts.contains(extensionFromContent.toLowerCase(Locale.ROOT));
        }
        return false;
    }
}
