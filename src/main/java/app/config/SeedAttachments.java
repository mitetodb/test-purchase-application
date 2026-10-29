package app.config;

import app.model.entity.Attachment;
import app.model.entity.TestPurchase;
import app.model.enums.AttachmentCategory;
import app.model.enums.AttachmentFileType;
import app.repository.AttachmentRepository;
import app.repository.TestPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Demo-only seed data: attaches a generated placeholder file to the first test purchase.
 * Disabled unless app.demo-data.enabled=true (env DEMO_DATA_ENABLED). Never reads host paths.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class SeedAttachments implements CommandLineRunner {

    private final AttachmentRepository attachmentRepository;
    private final TestPurchaseRepository testPurchaseRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void run(String... args) throws Exception {
        if (attachmentRepository.count() > 0) {
            return;
        }
        TestPurchase tp = testPurchaseRepository.findAll().stream().findFirst().orElse(null);
        if (tp == null) return;

        Path dir = Paths.get(uploadDir, "demo");
        Files.createDirectories(dir);
        Path file = dir.resolve("demo-offer.txt");
        if (!Files.exists(file)) {
            Files.writeString(file, "DEMO placeholder attachment - not real customer data.\n", StandardCharsets.UTF_8);
        }
        attachmentRepository.save(Attachment.builder()
                .testPurchase(tp)
                .name("demo-offer.txt")
                .category(AttachmentCategory.CURRENT_OFFER_PDF)
                .fileType(AttachmentFileType.PDF)
                .fileSize(Files.size(file))
                .filePath(file.toString())
                .build());
    }
}
