package com.pasalapeli.movie.repository;

import com.pasalapeli.movie.entity.Pelicula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeliculaRepository extends JpaRepository<Pelicula, Long> {
    @Query("SELECT DISTINCT p FROM Pelicula p LEFT JOIN FETCH p.funciones WHERE LOWER(p.titulo) LIKE LOWER(CONCAT('%', :busqueda, '%')) ORDER BY p.id DESC")
    List<Pelicula> buscarPorTituloConFunciones(@Param("busqueda") String busqueda);

    @Query("SELECT DISTINCT p FROM Pelicula p LEFT JOIN FETCH p.funciones ORDER BY p.id DESC")
    List<Pelicula> findAllWithFunciones();
}
