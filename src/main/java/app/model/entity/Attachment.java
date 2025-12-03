package app.model.entity;

import app.model.enums.AttachmentCategory;
import app.model.enums.AttachmentFileType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "attachments")
@Entity
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private TestPurchase testPurchase;

    private String name;

    @Enumerated(EnumType.STRING)
    private AttachmentCategory category;

    @Enumerated(EnumType.STRING)
    private AttachmentFileType fileType;

    private Long fileSize;

    private String filePath;

    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    private String updatedByUser;

    @PrePersist
    public void prePersist() {
        createdOn = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedOn = LocalDateTime.now();
    }
}
