package com.pasalapeli.movie.controller;

import com.pasalapeli.movie.dto.DisponibilidadDTO;
import com.pasalapeli.movie.dto.FuncionDTO;
import com.pasalapeli.movie.dto.FuncionRequestDTO;
import com.pasalapeli.movie.service.FuncionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/funciones")
@RequiredArgsConstructor
public class FuncionController {

    private final FuncionService funcionService;

    @GetMapping("/{id}")
    public ResponseEntity<FuncionDTO> obtenerFuncion(@PathVariable Long id) {
        return ResponseEntity.ok(funcionService.obtenerPorId(id));
    }

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<DisponibilidadDTO> consultarDisponibilidad(@PathVariable Long id) {
        return ResponseEntity.ok(funcionService.consultarDisponibilidad(id));
    }

    @PutMapping("/{id}/descontar")
    public ResponseEntity<DisponibilidadDTO> descontarEntradas(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int cantidad) {
        return ResponseEntity.ok(funcionService.descontarEntradas(id, cantidad));
    }

    @PutMapping("/{id}/reponer")
    public ResponseEntity<DisponibilidadDTO> reponerEntradas(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int cantidad) {
        return ResponseEntity.ok(funcionService.reponerEntradas(id, cantidad));
    }

    @PostMapping
    public ResponseEntity<FuncionDTO> crearFuncion(@Valid @RequestBody FuncionRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(funcionService.crearFuncion(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuncionDTO> actualizarFuncion(
            @PathVariable Long id,
            @Valid @RequestBody FuncionRequestDTO req) {
        return ResponseEntity.ok(funcionService.actualizarFuncion(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFuncion(@PathVariable Long id) {
        funcionService.eliminarFuncion(id);
        return ResponseEntity.noContent().build();
    }
}
