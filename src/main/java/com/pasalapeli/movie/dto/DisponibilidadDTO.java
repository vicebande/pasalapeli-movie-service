package com.pasalapeli.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadDTO {
    private Long funcionId;
    private Long peliculaId;
    private String peliculaTitulo;
    /** Genero de la pelicula: se propaga al ticket-service como "categoria" de la venta. */
    private String peliculaGenero;
    private String sala;
    private Integer entradasDisponibles;
    private BigDecimal precio;
    private boolean disponible;
    private LocalDate fecha;
    private LocalTime hora;
    private LocalDateTime finFuncion;
}
