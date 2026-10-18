package com.pasalapeli.movie.repository;

import com.pasalapeli.movie.entity.Funcion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionRepository extends JpaRepository<Funcion, Long> {
    List<Funcion> findByPeliculaId(Long peliculaId);

    boolean existsBySalaAndFechaAndHora(String sala, LocalDate fecha, LocalTime hora);

    boolean existsByIdNotAndSalaAndFechaAndHora(Long id, String sala, LocalDate fecha, LocalTime hora);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Funcion f WHERE f.id = :id")
    Optional<Funcion> findByIdForUpdate(@Param("id") Long id);

    /**
     * Borra solo si no quedan tickets: DELETE atómico con NOT EXISTS evita la
     * FK RESTRICT (una violación dentro de la transacción marcaría todo el
     * lote de la limpieza como rollback-only). Devuelve 0 filas si aún hay
     * tickets (se reintenta en el próximo ciclo).
     */
    @Modifying
    @Query(value = "DELETE FROM Funcion WHERE id = :id "
            + "AND NOT EXISTS (SELECT 1 FROM Ticket WHERE funcion_id = :id)",
            nativeQuery = true)
    int eliminarPorId(@Param("id") Long id);
}
