package com.pasalapeli.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
public class FuncionRequestDTO {
    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;

    @NotNull(message = "La hora es requerida")
    private LocalTime hora;

    @NotBlank(message = "La sala es requerida")
    private String sala;

    @NotNull(message = "Las entradas disponibles son requeridas")
    @PositiveOrZero(message = "Las entradas disponibles no pueden ser negativas")
    private Integer entradasDisponibles;

    @NotNull(message = "El precio es requerido")
    @Positive(message = "El precio debe ser mayor a cero")
    private BigDecimal precio;

    @NotNull(message = "El ID de película es requerido")
    private Long peliculaId;
}
