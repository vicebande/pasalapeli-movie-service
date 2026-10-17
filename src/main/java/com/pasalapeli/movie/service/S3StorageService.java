package com.pasalapeli.movie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Almacenamiento de portadas en Amazon S3 (bucket privado).
 * Conserva la interfaz publica del antiguo almacenamiento local: las URLs de la
 * BD siguen siendo "/uploads/<archivo>", por lo que ni la base de datos ni el
 * frontend cambian. El GET /uploads/{archivo} se resuelve con una URL
 * presignada de corta duracion (UploadsController).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    /** Nombres validos de archivo (generados por nosotros: UUID + extension). */
    private static final Pattern FILENAME = Pattern.compile("[A-Za-z0-9._-]+");

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    /** Guarda el archivo con nombre plano (UUID.ext) en uploads/; devuelve /uploads/<nombre>. */
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        extension = extension.replaceAll("[^a-zA-Z0-9.]", "");
        if (extension.length() > 10) {
            extension = "";
        }
        String fileName = UUID.randomUUID() + extension;
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key("uploads/" + fileName)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Portada subida a S3: s3://{}/uploads/{}", bucket, fileName);
            return "/uploads/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar archivo en S3: " + e.getMessage(), e);
        }
    }

    /** Elimina la portada de S3 (tolerante a errores: no bloquea el borrado de la pelicula). */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank() || !fileUrl.contains("/uploads/")) {
            return;
        }
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        if (!FILENAME.matcher(fileName).matches() || fileName.contains("..")) {
            log.warn("Nombre de archivo invalido para eliminar de S3: {}", fileUrl);
            return;
        }
        try {
            s3.deleteObject(b -> b.bucket(bucket).key("uploads/" + fileName));
            log.info("Portada eliminada de S3: uploads/{}", fileName);
        } catch (Exception e) {
            log.warn("No se pudo eliminar la portada de S3: {}", e.getMessage());
        }
    }

    /** URL presignada (GET, 30 min) para servir un archivo privado del bucket. */
    public String presignGet(String fileName) {
        if (fileName == null || fileName.contains("..") || !FILENAME.matcher(fileName).matches()) {
            throw new IllegalArgumentException("Nombre de archivo invalido: " + fileName);
        }
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key("uploads/" + fileName)
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofMinutes(30))
                .getObjectRequest(get));
        return presigned.url().toString();
    }
}
