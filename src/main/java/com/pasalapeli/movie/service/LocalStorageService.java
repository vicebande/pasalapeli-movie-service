package com.pasalapeli.movie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageService {

    @Value("${storage.local.upload-dir:./uploads/peliculas}")
    private String localUploadDir;

    public String uploadFile(MultipartFile file, String subFolder) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = subFolder + "/" + UUID.randomUUID() + extension;
        return saveLocally(file, fileName);
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        if (fileUrl.contains("/uploads/")) {
            try {
                String localName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                Path target = Paths.get(localUploadDir, localName);
                Files.deleteIfExists(target);
            } catch (IOException e) {
                log.warn("No se pudo eliminar archivo local: {}", e.getMessage());
            }
        }
    }

    private String saveLocally(MultipartFile file, String relativePath) {
        try {
            Path targetDir = Paths.get(localUploadDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            String cleanFileName = relativePath.replace("/", "_");
            Path targetPath = targetDir.resolve(cleanFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Archivo guardado localmente en: {}", targetPath.toAbsolutePath());
            return "/uploads/" + cleanFileName;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo en almacenamiento local: " + e.getMessage(), e);
        }
    }
}