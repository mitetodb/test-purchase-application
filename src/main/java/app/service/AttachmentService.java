package app.service;

import app.model.entity.Attachment;
import app.model.entity.TestPurchase;
import app.model.enums.AttachmentFileType;
import app.model.enums.AttachmentCategory;
import app.repository.AttachmentRepository;
import app.repository.TestPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TestPurchaseRepository testPurchaseRepository;
    private final FileStorageService storageService;

    public Attachment upload(UUID testPurchaseId,
                             MultipartFile file,
                             AttachmentCategory category) {

        TestPurchase tp = testPurchaseRepository.findById(testPurchaseId)
                .orElseThrow(() -> new IllegalArgumentException("TestPurchase not found"));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename().toLowerCase();
        AttachmentFileType fileType = determineFileType(filename);

        String storedPath = storageService.storeFile(file);

        Attachment att = Attachment.builder()
                .testPurchase(tp)
                .name(file.getOriginalFilename())
                .category(category == null ? AttachmentCategory.CURRENT_OFFER_PDF : category)
                .fileType(fileType)
                .fileSize(file.getSize())
                .filePath(storedPath)
                .build();

        return attachmentRepository.save(att);
    }

    public List<Attachment> getByTestPurchase(UUID tpId) {
        return attachmentRepository.findByTestPurchaseId(tpId);
    }

    public Attachment findById(UUID id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
    }

    public void delete(UUID id) {
        Attachment att = findById(id);
        storageService.delete(att.getFilePath());
        attachmentRepository.deleteById(id);
    }

    private AttachmentFileType determineFileType(String filename) {
        if (filename.endsWith(".pdf")) return AttachmentFileType.PDF;
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return AttachmentFileType.JPG;
        if (filename.endsWith(".doc") || filename.endsWith(".docx")) return AttachmentFileType.DOC;
        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) return AttachmentFileType.XLS;
        return AttachmentFileType.OTHER;
    }
}
