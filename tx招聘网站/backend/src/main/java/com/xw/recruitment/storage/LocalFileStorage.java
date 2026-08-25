package com.xw.recruitment.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorage implements FileStorage {
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private final Path root;

    public LocalFileStorage(@Value("${xw.storage.directory}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile save(MultipartFile file, String applicationNo) {
        if (file.isEmpty() || !ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Resume must be a PDF, DOC or DOCX file.");
        }
        String extension = switch (file.getContentType()) {
            case "application/pdf" -> ".pdf";
            case "application/msword" -> ".doc";
            default -> ".docx";
        };
        String key = applicationNo.toLowerCase(Locale.ROOT) + "/" + UUID.randomUUID() + extension;
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid storage path.");
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store resume.", exception);
        }
        return new StoredFile(key, safeFilename(file.getOriginalFilename()), file.getContentType(), file.getSize());
    }

    @Override
    public Resource load(String storageKey) {
        try {
            Path file = root.resolve(storageKey).normalize();
            if (!file.startsWith(root)) throw new IllegalArgumentException("Invalid storage path.");
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) throw new IllegalArgumentException("Resume not found.");
            return resource;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read resume.", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        Path file = root.resolve(storageKey).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Invalid storage path.");
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to delete resume.", exception);
        }
    }

    private String safeFilename(String name) {
        if (name == null || name.isBlank()) return "resume";
        return Path.of(name).getFileName().toString().replaceAll("[\\r\\n\"]", "_");
    }
}
