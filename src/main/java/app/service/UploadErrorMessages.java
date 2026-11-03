package app.service;

import app.exception.UploadRejectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class UploadErrorMessages {

    private final MessageSource messageSource;

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private DataSize maxFileSize;

    public String forRejected(UploadRejectedException ex, Locale locale) {
        String max = formatMaxSize(locale);
        String key = switch (ex.getTarget()) {
            case ATTACHMENT -> ex.getReason() == UploadRejectedException.Reason.TOO_LARGE
                    ? "error.upload.attachment.too_large"
                    : "error.upload.attachment";
            case AVATAR -> ex.getReason() == UploadRejectedException.Reason.TOO_LARGE
                    ? "error.upload.avatar.too_large"
                    : "error.upload.avatar";
        };
        return messageSource.getMessage(key, new Object[]{max}, locale);
    }

    public String attachmentError(Locale locale) {
        return messageSource.getMessage(
                "error.upload.attachment",
                new Object[]{formatMaxSize(locale)},
                locale);
    }

    public String attachmentTooLarge(Locale locale) {
        return messageSource.getMessage(
                "error.upload.attachment.too_large",
                new Object[]{formatMaxSize(locale)},
                locale);
    }

    public String avatarError(Locale locale) {
        return messageSource.getMessage(
                "error.upload.avatar",
                new Object[]{formatMaxSize(locale)},
                locale);
    }

    public String avatarTooLarge(Locale locale) {
        return messageSource.getMessage(
                "error.upload.avatar.too_large",
                new Object[]{formatMaxSize(locale)},
                locale);
    }

    public long getMaxFileSizeBytes() {
        return maxFileSize.toBytes();
    }

    public String formatMaxSizeDisplay(Locale locale) {
        return formatMaxSize(locale);
    }

    private String formatMaxSize(Locale locale) {
        long bytes = maxFileSize.toBytes();
        if (bytes < 1024 * 1024) {
            long kilobytes = Math.max(1, bytes / 1024);
            return messageSource.getMessage(
                    "error.upload.max_size_kb",
                    new Object[]{kilobytes},
                    kilobytes + " KB",
                    locale);
        }
        long megabytes = Math.max(1, maxFileSize.toMegabytes());
        return messageSource.getMessage(
                "error.upload.max_size_mb",
                new Object[]{megabytes},
                megabytes + " MB",
                locale);
    }
}
