package com.pasalapeli.movie.controller;

import com.pasalapeli.movie.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Sirve las portadas desde S3 mediante redirect 302 a una URL presignada.
 * Sustituye al resource-handler local de disco: el bucket es privado y las URLs
 * firmadas caducan (30 min de firma, 10 min de cache en el navegador).
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class UploadsController {

    private final S3StorageService storageService;

    @GetMapping("/uploads/{filename:[A-Za-z0-9._-]+}")
    public ResponseEntity<Void> servirPortada(@PathVariable String filename) {
        try {
            String url = storageService.presignGet(filename);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                    .build();
        } catch (Exception e) {
            log.warn("No se pudo generar la URL presignada para {}: {}", filename, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
