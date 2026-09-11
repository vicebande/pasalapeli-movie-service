package com.pasalapeli.movie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeliculaDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private String genero;
    private Integer duracion;
    private String clasificacion;
    private String imagen;
    private List<FuncionDTO> funciones;
}
