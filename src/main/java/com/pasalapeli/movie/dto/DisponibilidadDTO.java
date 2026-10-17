package com.pasalapeli.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadDTO {
    private Long funcionId;
    private Long peliculaId;
    private String peliculaTitulo;
    private String sala;
    private Integer entradasDisponibles;
    private BigDecimal precio;
    private boolean disponible;
    private LocalDate fecha;
    private LocalTime hora;
}
