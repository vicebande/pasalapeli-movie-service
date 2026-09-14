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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return funcionRepository.findByPeliculaId(peliculaId).stream()
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
                .build();
    }

    @Transactional
    public FuncionDTO crearFuncion(FuncionRequestDTO req) {
        Pelicula p = peliculaRepository.findById(req.getPeliculaId())
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con ID: " + req.getPeliculaId()));

        Funcion f = Funcion.builder()
                .fecha(req.getFecha())
                .hora(req.getHora())
                .sala(req.getSala())
                .entradasDisponibles(req.getEntradasDisponibles())
                .precio(req.getPrecio())
                .pelicula(p)
                .build();

        return mapToDTO(funcionRepository.save(f));
    }

    @Transactional
    public FuncionDTO actualizarFuncion(Long id, FuncionRequestDTO req) {
        Funcion f = funcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con ID: " + id));

        f.setFecha(req.getFecha());
        f.setHora(req.getHora());
        f.setSala(req.getSala());
        f.setEntradasDisponibles(req.getEntradasDisponibles());
        f.setPrecio(req.getPrecio());

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

    private FuncionDTO mapToDTO(Funcion f) {
        return FuncionDTO.builder()
                .id(f.getId())
                .fecha(f.getFecha())
                .hora(f.getHora())
                .sala(f.getSala())
                .entradasDisponibles(f.getEntradasDisponibles())
                .precio(f.getPrecio())
                .peliculaId(f.getPelicula() != null ? f.getPelicula().getId() : null)
                .peliculaTitulo(f.getPelicula() != null ? f.getPelicula().getTitulo() : null)
                .build();
    }
}
