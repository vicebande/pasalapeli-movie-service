package com.pasalapeli.movie.service;

import com.pasalapeli.movie.dto.FuncionDTO;
import com.pasalapeli.movie.dto.PeliculaDTO;
import com.pasalapeli.movie.dto.PeliculaRequestDTO;
import com.pasalapeli.movie.entity.Pelicula;
import com.pasalapeli.movie.exception.ResourceNotFoundException;
import com.pasalapeli.movie.repository.PeliculaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeliculaService {

    private final PeliculaRepository peliculaRepository;
    private final S3StorageService s3StorageService;

    @Transactional(readOnly = true)
    public List<PeliculaDTO> listarPeliculas(String busqueda) {
        List<Pelicula> peliculas;
        if (busqueda != null && !busqueda.isBlank()) {
            peliculas = peliculaRepository.findByTituloContainingIgnoreCase(busqueda);
        } else {
            peliculas = peliculaRepository.findAll();
        }
        return peliculas.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PeliculaDTO obtenerPorId(Long id) {
        Pelicula p = peliculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con ID: " + id));
        return mapToDTO(p);
    }

    @Transactional
    public PeliculaDTO crearPelicula(PeliculaRequestDTO request, MultipartFile imagenFile) {
        String imagenUrl = request.getImagen();
        if (imagenFile != null && !imagenFile.isEmpty()) {
            imagenUrl = s3StorageService.uploadFile(imagenFile, "peliculas");
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
                s3StorageService.deleteFile(existente.getImagen());
            }
            String nuevaUrl = s3StorageService.uploadFile(imagenFile, "peliculas");
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
        if (p.getImagen() != null) {
            s3StorageService.deleteFile(p.getImagen());
        }
        peliculaRepository.delete(p);
        log.info("Película eliminada con ID: {}", id);
    }

    @Transactional
    public String subirPortada(Long id, MultipartFile imagenFile) {
        Pelicula p = peliculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con ID: " + id));

        if (p.getImagen() != null) {
            s3StorageService.deleteFile(p.getImagen());
        }
        String nuevaUrl = s3StorageService.uploadFile(imagenFile, "peliculas");
        p.setImagen(nuevaUrl);
        peliculaRepository.save(p);
        return nuevaUrl;
    }

    private PeliculaDTO mapToDTO(Pelicula p) {
        List<FuncionDTO> funcionesDto = null;
        if (p.getFunciones() != null) {
            funcionesDto = p.getFunciones().stream().map(f -> FuncionDTO.builder()
                    .id(f.getId())
                    .fecha(f.getFecha())
                    .hora(f.getHora())
                    .sala(f.getSala())
                    .entradasDisponibles(f.getEntradasDisponibles())
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
