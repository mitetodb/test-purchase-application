package app.controller;

import app.model.entity.Attachment;
import app.model.enums.AttachmentFileType;
import app.exception.UploadRejectedException;
import app.service.AttachmentService;
import app.service.FileStorageService;
import app.service.UploadErrorMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final FileStorageService storageService;
    private final UploadErrorMessages uploadErrorMessages;

    @PostMapping("/upload/{testPurchaseId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','MYSTERY_SHOPPER')")
    public String upload(
            @PathVariable("testPurchaseId") UUID tpId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String categoryString,
            RedirectAttributes redirectAttributes
    ) {

        app.model.enums.AttachmentCategory category = null;
        if (categoryString != null && !categoryString.isBlank()) {
            category = app.model.enums.AttachmentCategory.valueOf(categoryString);
        }

        try {
            attachmentService.upload(tpId, file, category);
        } catch (UploadRejectedException ex) {
            redirectAttributes.addFlashAttribute(
                    "uploadError",
                    uploadErrorMessages.forRejected(ex, LocaleContextHolder.getLocale()));
            return "redirect:/testpurchases/view/" + tpId;
        }

        return "redirect:/testpurchases/view/" + tpId;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Attachment att = attachmentService.findById(id);
        PathResource resource = new PathResource(storageService.loadAsPath(att.getFilePath()));

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Sanitize filename for Content-Disposition header
        String sanitizedFilename = sanitizeFilename(att.getName());
        String contentDisposition = "attachment; filename=\"" + sanitizedFilename + "\"";
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    /**
     * Sanitize filename to prevent header injection and ensure ASCII safety.
     * Replaces non-ASCII characters with underscores and escapes quotes.
     */
    String sanitizeFilename(String filename) {
        if (filename == null) {
            return "download";
        }
        // Remove or replace dangerous characters
        return filename.replaceAll("[^\\x20-\\x7E]", "_")  // Replace non-ASCII with underscore
                .replace("\"", "")                          // Remove quotes
                .replace("\n", "")                          // Remove newlines
                .replace("\r", "");                         // Remove carriage returns
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable UUID id) {
        Attachment att = attachmentService.findById(id);
        UUID tpId = att.getTestPurchase().getId();
        attachmentService.delete(id);
        return "redirect:/testpurchases/view/" + tpId;
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewImage(@PathVariable UUID id) {
        Attachment att = attachmentService.findById(id);

        if (!(att.getFileType() == AttachmentFileType.JPG)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        PathResource resource = new PathResource(storageService.loadAsPath(att.getFilePath()));
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

}
