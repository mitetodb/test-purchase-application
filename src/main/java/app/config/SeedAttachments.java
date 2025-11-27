package app.config;

import app.model.entity.Attachment;
import app.model.entity.TestPurchase;
import app.model.enums.AttachmentCategory;
import app.model.enums.AttachmentFileType;
import app.repository.AttachmentRepository;
import app.repository.TestPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
public class SeedAttachments implements CommandLineRunner {

    private final AttachmentRepository attachmentRepository;
    private final TestPurchaseRepository testPurchaseRepository;

    @Override
    public void run(String... args) throws Exception {

        if (attachmentRepository.count() > 0) {
            return;
        }

        TestPurchase tp = testPurchaseRepository.findAll().stream().findFirst().orElse(null);
        if (tp == null) return;

        String path1 = "C:\\Users\\mitet\\Downloads\\file1.pdf";
        String path2 = "C:\\Users\\mitet\\Downloads\\file2.pdf";

        if (Files.exists(Paths.get(path1))) {
            Attachment a1 = Attachment.builder()
                    .testPurchase(tp)
                    .name("file1.pdf")
                    .category(AttachmentCategory.CANCELLATION)
                    .fileType(AttachmentFileType.PDF)
                    .fileSize(Files.size(Paths.get(path1)))
                    .filePath(path1)
                    .build();

            attachmentRepository.save(a1);
        }

        if (Files.exists(Paths.get(path2))) {
            Attachment a2 = Attachment.builder()
                    .testPurchase(tp)
                    .name("file2.pdf")
                    .category(AttachmentCategory.CURRENT_OFFER_PDF)
                    .fileType(AttachmentFileType.PDF)
                    .fileSize(Files.size(Paths.get(path2)))
                    .filePath(path2)
                    .build();

            attachmentRepository.save(a2);
        }
    }
}
