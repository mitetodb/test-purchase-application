package app.config;

import app.service.UploadErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class UploadFormModelAdvice {

    private final UploadErrorMessages uploadErrorMessages;

    @ModelAttribute("maxUploadFileBytes")
    public long maxUploadFileBytes() {
        return uploadErrorMessages.getMaxFileSizeBytes();
    }

    @ModelAttribute("maxUploadSizeDisplay")
    public String maxUploadSizeDisplay() {
        return uploadErrorMessages.formatMaxSizeDisplay(
                org.springframework.context.i18n.LocaleContextHolder.getLocale());
    }
}
