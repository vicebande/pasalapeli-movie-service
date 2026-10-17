package com.pasalapeli.movie.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Pelicula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 50)
    private String genero;

    @Column(nullable = false)
    private Integer duracion; // minutos

    @Column(nullable = false, length = 10)
    private String clasificacion;

    @Column(length = 255)
    private String imagen; // ruta /uploads/ o URL externa

    @OneToMany(mappedBy = "pelicula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<Funcion> funciones = new ArrayList<>();
}
