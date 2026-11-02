package app.service;

import app.model.enums.AttachmentFileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadContentValidatorTest {

    private UploadContentValidator validator;

    /** Smallest JFIF-style JPEG header (demo fixture). */
    private static final byte[] TINY_JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10,
            0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01,
            0x00, 0x01, 0x00, 0x00, (byte) 0xFF, (byte) 0xD9
    };

    /** Minimal PNG signature + IHDR chunk length/type (demo fixture). */
    private static final byte[] TINY_PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
            (byte) 0xDE
    };

    private static final byte[] TINY_PDF = "%PDF-1.4\n%%EOF\n".getBytes();

    @BeforeEach
    void setUp() {
        validator = new UploadContentValidator("jpeg,png,pdf");
    }

    @Test
    void acceptsValidJpeg() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", TINY_JPEG);

        UploadContentValidator.ValidatedUpload result = validator.validate(file);

        assertThat(result.fileType()).isEqualTo(AttachmentFileType.JPG);
        assertThat(result.extension()).isEqualTo(".jpg");
    }

    @Test
    void acceptsValidPng() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.png", "image/png", TINY_PNG);

        UploadContentValidator.ValidatedUpload result = validator.validate(file);

        assertThat(result.fileType()).isEqualTo(AttachmentFileType.PNG);
        assertThat(result.extension()).isEqualTo(".png");
    }

    @Test
    void acceptsValidPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", TINY_PDF);

        UploadContentValidator.ValidatedUpload result = validator.validate(file);

        assertThat(result.fileType()).isEqualTo(AttachmentFileType.PDF);
        assertThat(result.extension()).isEqualTo(".pdf");
    }

    @Test
    void rejectsSpoofedPdfExtensionWithTextContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "not a pdf".getBytes());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void rejectsRenamedJpegWithPdfExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", TINY_JPEG);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File content does not match the file extension.");
    }

    @Test
    void rejectsMisleadingContentTypeHeader() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "application/pdf", TINY_JPEG);

        UploadContentValidator.ValidatedUpload result = validator.validate(file);

        assertThat(result.fileType()).isEqualTo(AttachmentFileType.JPG);
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File is empty");
    }
}
