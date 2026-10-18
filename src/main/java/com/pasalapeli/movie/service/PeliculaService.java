package com.pasalapeli.movie.service;

import com.pasalapeli.movie.dto.FuncionDTO;
import com.pasalapeli.movie.dto.PeliculaDTO;
import com.pasalapeli.movie.dto.PeliculaRequestDTO;
import com.pasalapeli.movie.entity.Pelicula;
import com.pasalapeli.movie.event.EventoNotificacion;
import com.pasalapeli.movie.exception.ResourceNotFoundException;
import com.pasalapeli.movie.messaging.NotificacionEventPublisher;
import com.pasalapeli.movie.repository.PeliculaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeliculaService {

    private final PeliculaRepository peliculaRepository;
    private final S3StorageService storageService;
    private final NotificacionEventPublisher notificacionEventPublisher;

    @Transactional(readOnly = true)
    public List<PeliculaDTO> listarPeliculas(String busqueda, boolean todas) {
        List<Pelicula> peliculas;
        if (busqueda != null && !busqueda.isBlank()) {
            peliculas = peliculaRepository.buscarPorTituloConFunciones(busqueda);
        } else {
            peliculas = peliculaRepository.findAllWithFunciones();
        }
        // Catálogo público: oculta películas sin funciones próximas (todas=true las muestra, p. ej. el admin)
        return peliculas.stream()
                .map(this::mapToDTO)
                .filter(dto -> todas || (dto.getFunciones() != null && !dto.getFunciones().isEmpty()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PeliculaDTO obtenerPorId(Long id) {
        Pelicula p = peliculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con ID: " + id));
        PeliculaDTO dto = mapToDTO(p);
        // Protección de acceso directo (/pelicula/:id): sin funciones próximas no está disponible
        if (dto.getFunciones() == null || dto.getFunciones().isEmpty()) {
            throw new ResourceNotFoundException("Película no disponible en la cartelera con ID: " + id);
        }
        return dto;
    }

    @Transactional
    public PeliculaDTO crearPelicula(PeliculaRequestDTO request, MultipartFile imagenFile) {
        String imagenUrl = request.getImagen();
if (imagenFile != null && !imagenFile.isEmpty()) {
            imagenUrl = storageService.uploadFile(imagenFile);
        }
        Pelicula pelicula = Pelicula.builder()
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .genero(request.getGenero())
                .duracion(request.getDuracion())
                .clasificacion(request.getClasificacion())
                .imagen(imagenUrl)
                .build();

        Pelicula guardada = peliculaRepository.save(pelicula);
        log.info("Película creada con ID: {}", guardada.getId());

        // Evento asincronico: notifica a los usuarios la nueva pelicula en cartelera.
        notificacionEventPublisher.publicar(EventoNotificacion.builder()
                .tipo("PELICULA_CREADA")
                .peliculaId(guardada.getId())
                .tituloPelicula(guardada.getTitulo())
                .build());

        return mapToDTO(guardada);
    }

    @Transactional
    public PeliculaDTO actualizarPelicula(Long id, PeliculaRequestDTO request, MultipartFile imagenFile) {
        Pelicula existente = peliculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con ID: " + id));

        existente.setTitulo(request.getTitulo());
        existente.setDescripcion(request.getDescripcion());
        existente.setGenero(request.getGenero());
        existente.setDuracion(request.getDuracion());
        existente.setClasificacion(request.getClasificacion());

        if (imagenFile != null && !imagenFile.isEmpty()) {
            if (existente.getImagen() != null) {
                eliminarImagenTrasCommit(existente.getImagen());
            }
            String nuevaUrl = storageService.uploadFile(imagenFile);
            existente.setImagen(nuevaUrl);
        } else if (request.getImagen() != null) {
            existente.setImagen(request.getImagen());
        }

        return mapToDTO(peliculaRepository.save(existente));
    }

    @Transactional
    public void eliminarPelicula(Long id) {
        Pelicula p = peliculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con ID: " + id));

        // Evento asincronico: avisa a los usuarios que la pelicula dejo de estar disponible.
        notificacionEventPublisher.publicar(EventoNotificacion.builder()
                .tipo("PELICULA_ELIMINADA")
                .peliculaId(p.getId())
                .tituloPelicula(p.getTitulo())
                .build());

        if (p.getImagen() != null) {
            eliminarImagenTrasCommit(p.getImagen());
        }
        peliculaRepository.delete(p);
        log.info("Película eliminada con ID: {}", id);
    }

    @Transactional
    public String subirPortada(Long id, MultipartFile imagenFile) {
        Pelicula p = peliculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con ID: " + id));

        if (p.getImagen() != null) {
            eliminarImagenTrasCommit(p.getImagen());
        }
        String nuevaUrl = storageService.uploadFile(imagenFile);
        p.setImagen(nuevaUrl);
        peliculaRepository.save(p);
        return nuevaUrl;
    }

    /**
     * Borra la imagen fisica solo si la transaccion se confirma: si el commit falla,
     * el registro en BD sigue apuntando al archivo y no queda un enlace muerto.
     */
    private void eliminarImagenTrasCommit(String imagenUrl) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storageService.deleteFile(imagenUrl);
                }
            });
        } else {
            storageService.deleteFile(imagenUrl);
        }
    }

    private PeliculaDTO mapToDTO(Pelicula p) {
        List<FuncionDTO> funcionesDto = null;
        if (p.getFunciones() != null) {
            // Solo funciones futuras: las ya iniciadas no se ofrecen en catálogo ni en detalle
            LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Santiago"));
            funcionesDto = p.getFunciones().stream()
                    .filter(f -> LocalDateTime.of(f.getFecha(), f.getHora()).isAfter(ahora))
                    .map(f -> FuncionDTO.builder()
                    .id(f.getId())
                    .fecha(f.getFecha())
                    .hora(f.getHora())
                    .sala(f.getSala())
                    .entradasDisponibles(f.getEntradasDisponibles())
                    .aforo(f.getAforo())
                    .precio(f.getPrecio())
                    .peliculaId(p.getId())
                    .peliculaTitulo(p.getTitulo())
                    .build()).collect(Collectors.toList());
        }

        return PeliculaDTO.builder()
                .id(p.getId())
                .titulo(p.getTitulo())
                .descripcion(p.getDescripcion())
                .genero(p.getGenero())
                .duracion(p.getDuracion())
                .clasificacion(p.getClasificacion())
                .imagen(p.getImagen())
                .funciones(funcionesDto)
                .build();
    }
}
