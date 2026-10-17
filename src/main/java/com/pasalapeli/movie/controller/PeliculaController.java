package com.pasalapeli.movie.controller;

import com.pasalapeli.movie.dto.FuncionDTO;
import com.pasalapeli.movie.dto.PeliculaDTO;
import com.pasalapeli.movie.dto.PeliculaRequestDTO;
import com.pasalapeli.movie.service.FuncionService;
import com.pasalapeli.movie.service.PeliculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class PeliculaController {

    private final PeliculaService peliculaService;
    private final FuncionService funcionService;

    @GetMapping
    public ResponseEntity<List<PeliculaDTO>> listarPeliculas(@RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(peliculaService.listarPeliculas(busqueda));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeliculaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(peliculaService.obtenerPorId(id));
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PeliculaDTO> crearPeliculaJson(@Valid @RequestBody PeliculaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(peliculaService.crearPelicula(request, null));
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<PeliculaDTO> crearPeliculaMultipart(
            @RequestPart("datos") @Valid PeliculaRequestDTO request,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.status(HttpStatus.CREATED).body(peliculaService.crearPelicula(request, imagen));
    }

    @PutMapping(value = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<PeliculaDTO> actualizarPeliculaJson(
            @PathVariable Long id,
            @Valid @RequestBody PeliculaRequestDTO request) {
        return ResponseEntity.ok(peliculaService.actualizarPelicula(id, request, null));
    }

    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<PeliculaDTO> actualizarPeliculaMultipart(
            @PathVariable Long id,
            @RequestPart("datos") @Valid PeliculaRequestDTO request,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.ok(peliculaService.actualizarPelicula(id, request, imagen));
    }

    @PostMapping("/{id}/imagen")
    public ResponseEntity<Map<String, String>> subirPortada(
            @PathVariable Long id,
            @RequestParam("imagen") MultipartFile imagen) {
        String url = peliculaService.subirPortada(id, imagen);
        return ResponseEntity.ok(Map.of("url", url, "mensaje", "Portada subida exitosamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPelicula(@PathVariable Long id) {
        peliculaService.eliminarPelicula(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/funciones")
    public ResponseEntity<List<FuncionDTO>> listarFuncionesDePelicula(@PathVariable Long id) {
        return ResponseEntity.ok(funcionService.listarPorPelicula(id));
    }
}
