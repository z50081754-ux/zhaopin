package com.xw.recruitment.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    StoredFile save(MultipartFile file, String applicationNo);
    Resource load(String storageKey);
    void delete(String storageKey);
    record StoredFile(String storageKey, String filename, String contentType, long size) {}
}
