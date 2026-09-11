package com.pasalapeli.movie.repository;

import com.pasalapeli.movie.entity.Pelicula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeliculaRepository extends JpaRepository<Pelicula, Long> {
    List<Pelicula> findByTituloContainingIgnoreCase(String titulo);
    List<Pelicula> findByGeneroContainingIgnoreCase(String genero);

    @Query("SELECT DISTINCT p FROM Pelicula p LEFT JOIN FETCH p.funciones")
    List<Pelicula> findAllWithFunciones();
}
