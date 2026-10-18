package com.pasalapeli.movie.service;

import com.pasalapeli.movie.dto.DisponibilidadDTO;
import com.pasalapeli.movie.dto.FuncionDTO;
import com.pasalapeli.movie.dto.FuncionRequestDTO;
import com.pasalapeli.movie.entity.Funcion;
import com.pasalapeli.movie.entity.Pelicula;
import com.pasalapeli.movie.exception.InsufficientTicketsException;
import com.pasalapeli.movie.exception.ResourceNotFoundException;
import com.pasalapeli.movie.repository.FuncionRepository;
import com.pasalapeli.movie.repository.PeliculaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuncionService {

    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;

    @Transactional(readOnly = true)
    public List<FuncionDTO> listarPorPelicula(Long peliculaId) {
        // Solo funciones cuya proyección aún no comienza (catálogo público y tabla admin)
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Santiago"));
        return funcionRepository.findByPeliculaId(peliculaId).stream()
                .filter(f -> LocalDateTime.of(f.getFecha(), f.getHora()).isAfter(ahora))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FuncionDTO obtenerPorId(Long id) {
        Funcion f = funcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con ID: " + id));
        return mapToDTO(f);
    }

    @Transactional(readOnly = true)
    public DisponibilidadDTO consultarDisponibilidad(Long funcionId) {
        Funcion f = funcionRepository.findById(funcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con ID: " + funcionId));

        boolean disponible = f.getEntradasDisponibles() != null && f.getEntradasDisponibles() > 0;

        return DisponibilidadDTO.builder()
                .funcionId(f.getId())
                .peliculaId(f.getPelicula().getId())
                .peliculaTitulo(f.getPelicula().getTitulo())
                .sala(f.getSala())
                .entradasDisponibles(f.getEntradasDisponibles())
                .precio(f.getPrecio())
                .disponible(disponible)
                .fecha(f.getFecha())
                .hora(f.getHora())
                .finFuncion(finFuncion(f))
                .build();
    }

    @Transactional
    public DisponibilidadDTO descontarEntradas(Long funcionId, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a 0");
        }

        // Bloqueo pesimista para consistencia en alta concurrencia
        Funcion f = funcionRepository.findByIdForUpdate(funcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con ID: " + funcionId));

        if (f.getEntradasDisponibles() < cantidad) {
            log.warn("Entradas insuficientes para función {}: Solicitadas {}, Disponibles {}",
                    funcionId, cantidad, f.getEntradasDisponibles());
            throw new InsufficientTicketsException(
                    String.format("No hay cupos disponibles. Solicitados: %d, Disponibles: %d",
                            cantidad, f.getEntradasDisponibles())
            );
        }

        f.setEntradasDisponibles(f.getEntradasDisponibles() - cantidad);
        Funcion actualizada = funcionRepository.save(f);
        log.info("Descontadas {} entradas para función {}. Restantes: {}",
                cantidad, funcionId, actualizada.getEntradasDisponibles());

        return DisponibilidadDTO.builder()
                .funcionId(actualizada.getId())
                .peliculaId(actualizada.getPelicula().getId())
                .peliculaTitulo(actualizada.getPelicula().getTitulo())
                .sala(actualizada.getSala())
                .entradasDisponibles(actualizada.getEntradasDisponibles())
                .precio(actualizada.getPrecio())
                .disponible(actualizada.getEntradasDisponibles() > 0)
                .fecha(actualizada.getFecha())
                .hora(actualizada.getHora())
                .build();
    }

    @Transactional
    public DisponibilidadDTO reponerEntradas(Long funcionId, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer debe ser mayor a 0");
        }

        // Bloqueo pesimista para consistencia con compras concurrentes
        Funcion f = funcionRepository.findByIdForUpdate(funcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con ID: " + funcionId));

        int nuevasDisponibles = f.getEntradasDisponibles() + cantidad;
        if (f.getAforo() != null && f.getAforo() > 0 && nuevasDisponibles > f.getAforo()) {
            throw new IllegalArgumentException(
                    String.format("No se puede exceder el aforo de la sala. Disponibles: %d, a sumar: %d, aforo: %d",
                            f.getEntradasDisponibles(), cantidad, f.getAforo()));
        }

        f.setEntradasDisponibles(nuevasDisponibles);
        Funcion actualizada = funcionRepository.save(f);
        log.info("Repuestas {} entradas para función {}. Disponibles: {}",
                cantidad, funcionId, actualizada.getEntradasDisponibles());

        return DisponibilidadDTO.builder()
                .funcionId(actualizada.getId())
                .peliculaId(actualizada.getPelicula().getId())
                .peliculaTitulo(actualizada.getPelicula().getTitulo())
                .sala(actualizada.getSala())
                .entradasDisponibles(actualizada.getEntradasDisponibles())
                .precio(actualizada.getPrecio())
                .disponible(actualizada.getEntradasDisponibles() > 0)
                .fecha(actualizada.getFecha())
                .hora(actualizada.getHora())
                .build();
    }

    @Transactional
    public FuncionDTO crearFuncion(FuncionRequestDTO req) {
        Pelicula p = peliculaRepository.findById(req.getPeliculaId())
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con ID: " + req.getPeliculaId()));

        if (funcionRepository.existsBySalaAndFechaAndHora(req.getSala(), req.getFecha(), req.getHora())) {
            throw new IllegalArgumentException(
                    "Ya existe una función en la sala " + req.getSala() + " el " + req.getFecha() + " a las " + req.getHora());
        }

        Integer aforo = req.getAforo() != null ? req.getAforo() : req.getEntradasDisponibles();

        Funcion f = Funcion.builder()
                .fecha(req.getFecha())
                .hora(req.getHora())
                .sala(req.getSala())
                .entradasDisponibles(req.getEntradasDisponibles())
                .aforo(aforo)
                .precio(req.getPrecio())
                .pelicula(p)
                .build();

        return mapToDTO(funcionRepository.save(f));
    }

    @Transactional
    public FuncionDTO actualizarFuncion(Long id, FuncionRequestDTO req) {
        Funcion f = funcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con ID: " + id));

        if (funcionRepository.existsByIdNotAndSalaAndFechaAndHora(id, req.getSala(), req.getFecha(), req.getHora())) {
            throw new IllegalArgumentException(
                    "Ya existe otra función en la sala " + req.getSala() + " el " + req.getFecha() + " a las " + req.getHora());
        }

        f.setFecha(req.getFecha());
        f.setHora(req.getHora());
        f.setSala(req.getSala());
        f.setPrecio(req.getPrecio());
        // entradasDisponibles NO se pisa: lo manejan descontar/reponer (compras y devoluciones)
        if (req.getAforo() != null) {
            f.setAforo(req.getAforo());
        }

        Funcion actualizada = funcionRepository.save(f);
        log.info("Función actualizada con ID: {} para película {}", id, actualizada.getPelicula().getId());
        return mapToDTO(actualizada);
    }

    @Transactional
    public void eliminarFuncion(Long id) {
        Funcion f = funcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con ID: " + id));
        funcionRepository.delete(f);
        log.info("Función eliminada con ID: {}", id);
    }

    private LocalDateTime finFuncion(Funcion f) {
        int duracion = f.getPelicula() != null && f.getPelicula().getDuracion() != null
                ? f.getPelicula().getDuracion() : 0;
        return LocalDateTime.of(f.getFecha(), f.getHora()).plusMinutes(duracion);
    }

    /**
     * Limpieza programada: elimina las funciones cuya proyección ya terminó
     * (fecha + hora + duración <= ahora America/Santiago). Solo se borra si no
     * queda ningún ticket asociado (FK RESTRICT): si aún tiene tickets, Ticket
     * Service los purga después y este job reintenta en el próximo ciclo.
     */
    @Scheduled(fixedDelayString = "${app.limpieza.intervalo-ms:300000}",
            initialDelayString = "${app.limpieza.retraso-inicial-ms:45000}")
    @Transactional
    public void purgarFuncionesFinalizadas() {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Santiago"));
        List<Long> expiradas = new ArrayList<>();
        for (Funcion f : funcionRepository.findAll()) {
            if (!finFuncion(f).isAfter(ahora)) {
                expiradas.add(f.getId());
            }
        }
        int purgadas = 0;
        int pendientes = 0;
        for (Long id : expiradas) {
            try {
                funcionRepository.eliminarPorId(id);
                purgadas++;
            } catch (DataAccessException e) {
                // FK RESTRICT: la función todavía tiene tickets; se reintenta en el próximo ciclo
                pendientes++;
            }
        }
        if (purgadas > 0 || pendientes > 0) {
            log.info("Limpieza: {} función(es) finalizada(s) eliminadas, {} aún con tickets (pendientes).",
                    purgadas, pendientes);
        }
    }

    private FuncionDTO mapToDTO(Funcion f) {
        return FuncionDTO.builder()
                .id(f.getId())
                .fecha(f.getFecha())
                .hora(f.getHora())
                .sala(f.getSala())
                .entradasDisponibles(f.getEntradasDisponibles())
                .aforo(f.getAforo())
                .precio(f.getPrecio())
                .peliculaId(f.getPelicula() != null ? f.getPelicula().getId() : null)
                .peliculaTitulo(f.getPelicula() != null ? f.getPelicula().getTitulo() : null)
                .build();
    }
}
