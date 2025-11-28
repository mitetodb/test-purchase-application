package app.controller;

import app.model.entity.Attachment;
import app.model.enums.AttachmentFileType;
import app.service.AttachmentService;
import app.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final FileStorageService storageService;

    @PostMapping("/upload")
    public String upload(
            @RequestParam("testPurchaseId") UUID tpId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String categoryString
    ) {

        app.model.enums.AttachmentCategory category = null;
        if (categoryString != null && !categoryString.isBlank()) {
            category = app.model.enums.AttachmentCategory.valueOf(categoryString);
        }

        attachmentService.upload(tpId, file, category);

        return "redirect:/testpurchases/view/" + tpId;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Attachment att = attachmentService.findById(id);
        PathResource resource = new PathResource(storageService.loadAsPath(att.getFilePath()));

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentDisposition = "attachment; filename=\"" + att.getName() + "\"";
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
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
