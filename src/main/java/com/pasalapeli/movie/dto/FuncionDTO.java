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
public class FuncionDTO {
    private Long id;
    private LocalDate fecha;
    private LocalTime hora;
    private String sala;
    private Integer entradasDisponibles;
    private BigDecimal precio;
    private Long peliculaId;
    private String peliculaTitulo;
}
