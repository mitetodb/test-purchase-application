package app.service;

import app.config.SecurityUtils;
import app.exception.UploadRejectedException;
import app.model.entity.Attachment;
import app.model.entity.TestPurchase;
import app.model.enums.AttachmentCategory;
import app.model.enums.AttachmentFileType;
import app.repository.AttachmentRepository;
import app.repository.TestPurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceUploadValidationTest {

    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private TestPurchaseRepository testPurchaseRepository;
    @Mock
    private FileStorageService storageService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UploadContentValidator uploadContentValidator;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AttachmentService attachmentService;

    private UUID purchaseId;
    private TestPurchase testPurchase;

    private static final byte[] TINY_PDF = "%PDF-1.4\n%%EOF\n".getBytes();

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();
        testPurchase = TestPurchase.builder().id(purchaseId).build();
        when(testPurchaseRepository.findById(purchaseId)).thenReturn(Optional.of(testPurchase));
    }

    @Test
    void uploadRejectsInvalidFileBeforeStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.pdf", "application/pdf", "plain text".getBytes());
        when(uploadContentValidator.validateForAttachment(file))
                .thenThrow(new UploadRejectedException(
                        UploadRejectedException.Reason.TYPE_NOT_ALLOWED,
                        UploadRejectedException.UploadTarget.ATTACHMENT));

        try (var ignored = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            ignored.when(SecurityUtils::getCurrentUsername).thenReturn("tester");

            assertThatThrownBy(() -> attachmentService.upload(
                    purchaseId, file, AttachmentCategory.INVOICE))
                    .isInstanceOf(UploadRejectedException.class);
        }

        verify(storageService, never()).storeFile(any(), any());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void uploadStoresWhenValidationPasses() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", TINY_PDF);
        UploadContentValidator.ValidatedUpload validated =
                new UploadContentValidator.ValidatedUpload(AttachmentFileType.PDF, ".pdf");
        when(uploadContentValidator.validateForAttachment(file)).thenReturn(validated);
        when(storageService.storeFile(file, ".pdf")).thenReturn("/tmp/stored.pdf");
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> inv.getArgument(0));

        try (var ignored = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            ignored.when(SecurityUtils::getCurrentUsername).thenReturn("tester");

            attachmentService.upload(purchaseId, file, AttachmentCategory.INVOICE);
        }

        verify(storageService).storeFile(eq(file), eq(".pdf"));
        verify(attachmentRepository).save(any(Attachment.class));
    }
}
