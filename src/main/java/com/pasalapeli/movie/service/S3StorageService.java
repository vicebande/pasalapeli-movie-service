package com.pasalapeli.movie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class S3StorageService {

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${aws.s3.bucket-name:pasalapeli-portadas}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.local-storage.upload-dir:./uploads/peliculas}")
    private String localUploadDir;

    @Autowired(required = false)
    private S3Client s3Client;

    public String uploadFile(MultipartFile file, String subFolder) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = subFolder + "/" + UUID.randomUUID() + extension;

        if (s3Enabled && s3Client != null) {
            try {
                log.info("Subiendo archivo a Amazon S3 Bucket '{}': {}", bucketName, fileName);
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
                log.info("Archivo subido exitosamente a S3: {}", s3Url);
                return s3Url;
            } catch (Exception e) {
                log.error("Error al subir a Amazon S3, aplicando fallback local: {}", e.getMessage());
            }
        }

        // Fallback local si S3 no está activado o falla
        return saveLocally(file, fileName);
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        if (s3Enabled && s3Client != null && fileUrl.contains(bucketName)) {
            try {
                String key = fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);
                if (key.startsWith("/")) key = key.substring(1);
                log.info("Eliminando archivo de Amazon S3: {}", key);
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteRequest);
                return;
            } catch (Exception e) {
                log.error("Error al eliminar objeto en S3: {}", e.getMessage());
            }
        }

        // Si es archivo local
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
