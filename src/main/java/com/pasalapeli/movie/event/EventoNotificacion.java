package com.pasalapeli.movie.event;

import lombok.*;

import java.math.BigDecimal;

/**
 * Evento de notificacion publicado de forma asincrona hacia AWS SNS.
 * El microservicio de notificaciones lo consume (deserializa por nombre de campo).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoNotificacion {

    /** PELICULA_CREADA | PELICULA_ELIMINADA | TICKET_PAGADO | TICKET_DEVUELTO */
    private String tipo;

    private Long usuarioId;
    private Long peliculaId;
    private String tituloPelicula;
    private String codigoTicket;
    private Integer cantidad;
    private BigDecimal monto;
    private String sala;
}
