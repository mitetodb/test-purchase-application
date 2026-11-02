package app.service;

import app.exception.UploadRejectedException;
import app.model.enums.AttachmentFileType;
import app.support.UploadFixtureBytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadContentValidatorTest {

    private UploadContentValidator attachmentValidator;

    @BeforeEach
    void setUp() {
        attachmentValidator = new UploadContentValidator(
                "jpeg,png,pdf,docx,xlsx",
                "jpeg,png",
                DataSize.ofKilobytes(512));
    }

    @Test
    void acceptsValidJpegForAttachment() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", UploadFixtureBytes.TINY_JPEG);

        UploadContentValidator.ValidatedUpload result = attachmentValidator.validateForAttachment(file);

        assertThat(result.fileType()).isEqualTo(AttachmentFileType.JPG);
        assertThat(result.extension()).isEqualTo(".jpg");
    }

    @Test
    void acceptsDocxAndXlsxForAttachment() throws Exception {
        MockMultipartFile docx = new MockMultipartFile(
                "file", "notes.docx", "application/octet-stream", UploadFixtureBytes.minimalDocx());
        MockMultipartFile xlsx = new MockMultipartFile(
                "file", "sheet.xlsx", "application/octet-stream", UploadFixtureBytes.minimalXlsx());

        assertThat(attachmentValidator.validateForAttachment(docx).fileType()).isEqualTo(AttachmentFileType.DOC);
        assertThat(attachmentValidator.validateForAttachment(xlsx).fileType()).isEqualTo(AttachmentFileType.XLS);
    }

    @Test
    void rejectsLegacyDocAndXls() {
        MockMultipartFile doc = new MockMultipartFile(
                "file", "legacy.doc", "application/msword", UploadFixtureBytes.LEGACY_OLE);
        MockMultipartFile xls = new MockMultipartFile(
                "file", "legacy.xls", "application/vnd.ms-excel", UploadFixtureBytes.LEGACY_OLE);

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(doc))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TYPE_NOT_ALLOWED);

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(xls))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TYPE_NOT_ALLOWED);
    }

    @Test
    void rejectsDocmRenamedAsDocx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.docx", "application/octet-stream", UploadFixtureBytes.minimalDocm());

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(file))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TYPE_NOT_ALLOWED);
    }

    @Test
    void rejectsXlsmRenamedAsXlsx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.xlsx", "application/octet-stream", UploadFixtureBytes.minimalXlsm());

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(file))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TYPE_NOT_ALLOWED);
    }

    @Test
    void rejectsPlainZipRenamedAsDocx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.docx", "application/octet-stream", UploadFixtureBytes.plainZipWithTextFile());

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(file))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TYPE_NOT_ALLOWED);
    }

    @Test
    void rejectsMacroDocmAndXlsm() throws Exception {
        MockMultipartFile docm = new MockMultipartFile(
                "file", "macro.docm", "application/octet-stream", UploadFixtureBytes.minimalDocm());
        MockMultipartFile xlsm = new MockMultipartFile(
                "file", "macro.xlsm", "application/octet-stream", UploadFixtureBytes.minimalXlsm());

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(docm))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TYPE_NOT_ALLOWED);

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(xlsm))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TYPE_NOT_ALLOWED);
    }

    @Test
    void rejectsSpoofedPdfExtensionWithTextContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "not a pdf".getBytes());

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(file))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TYPE_NOT_ALLOWED);
    }

    @Test
    void rejectsRenamedJpegWithPdfExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", UploadFixtureBytes.TINY_JPEG);

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(file))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.EXTENSION_MISMATCH);
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(file))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.EMPTY);
    }

    @Test
    void rejectsOversizeBeforeStorage() {
        byte[] big = new byte[600 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", big);

        assertThatThrownBy(() -> attachmentValidator.validateForAttachment(file))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TOO_LARGE);
    }

    @Test
    void avatarRejectsPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.pdf", "application/pdf", UploadFixtureBytes.TINY_PDF);

        assertThatThrownBy(() -> attachmentValidator.validateForAvatar(file))
                .isInstanceOf(UploadRejectedException.class)
                .extracting("reason")
                .isEqualTo(UploadRejectedException.Reason.TYPE_NOT_ALLOWED);
    }

    @Test
    void avatarAcceptsPng() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", UploadFixtureBytes.TINY_PNG);

        assertThat(attachmentValidator.validateForAvatar(file).fileType()).isEqualTo(AttachmentFileType.PNG);
    }
}
