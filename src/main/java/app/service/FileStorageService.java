package app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public String storeFile(MultipartFile file) {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";

        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot);
        }

        String filename = UUID.randomUUID().toString() + ext;

        try {
            if (original.contains("..")) {
                throw new IllegalArgumentException("Invalid path sequence in file name " + original);
            }

            Path target = this.uploadRoot.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return target.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + original, ex);
        }
    }

    public Path loadAsPath(String filePath) {
        return Paths.get(filePath).toAbsolutePath();
    }

    public boolean delete(String filePath) {
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }
}
