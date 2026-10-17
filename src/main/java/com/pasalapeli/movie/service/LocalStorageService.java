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

    /** Guarda el archivo con nombre plano (UUID.ext) dentro de upload-dir; sin subcarpetas. */
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // Extension saneada: solo caracteres seguros
        extension = extension.replaceAll("[^a-zA-Z0-9.]", "");
        if (extension.length() > 10) {
            extension = "";
        }
        String fileName = UUID.randomUUID() + extension;
        return saveLocally(file, fileName);
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        if (fileUrl.contains("/uploads/")) {
            try {
                String localName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                Path base = Paths.get(localUploadDir).toAbsolutePath().normalize();
                Path target = base.resolve(localName).normalize();
                // Path containment: rechaza cualquier intento de salirse de upload-dir
                if (!target.startsWith(base)) {
                    log.warn("Ruta de borrado fuera del directorio permitido: {}", fileUrl);
                    return;
                }
                Files.deleteIfExists(target);
            } catch (IOException e) {
                log.warn("No se pudo eliminar archivo local: {}", e.getMessage());
            }
        }
    }

    private Path resolveSafely(String fileName) {
        Path base = Paths.get(localUploadDir).toAbsolutePath().normalize();
        Path target = base.resolve(fileName).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalStateException("Nombre de archivo inválido: " + fileName);
        }
        return target;
    }

    private String saveLocally(MultipartFile file, String fileName) {
        try {
            Path targetDir = Paths.get(localUploadDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            Path targetPath = resolveSafely(fileName);
            try (java.io.InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Archivo guardado localmente en: {}", targetPath.toAbsolutePath());
            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo en almacenamiento local: " + e.getMessage(), e);
        }
    }
}
