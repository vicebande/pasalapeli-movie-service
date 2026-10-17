package com.pasalapeli.movie.messaging;

import com.pasalapeli.movie.event.EventoNotificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica eventos de notificacion hacia RabbitMQ de forma desacoplada de la
 * logica de negocio. El envio real ocurre AFTER_COMMIT para no notificar por
 * transacciones que finalmente revierten, y los errores del broker no rompen
 * la operacion de negocio.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RabbitTemplate rabbitTemplate;

    @Value("${notificaciones.exchange:pasalapeli.events}")
    private String exchange;

    /** Encola el evento para publicarlo cuando la transaccion confirme. */
    public void publicar(EventoNotificacion evento) {
        applicationEventPublisher.publishEvent(evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(EventoNotificacion evento) {
        String routingKey = routingKey(evento.getTipo());
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento);
            log.info("Evento publicado en RabbitMQ: {} (exchange={}, rk={})",
                    evento.getTipo(), exchange, routingKey);
        } catch (Exception ex) {
            // La mensajeria es best-effort: no debe tumbar la transaccion de negocio.
            log.error("No se pudo publicar el evento {} en RabbitMQ: {}", evento.getTipo(), ex.getMessage());
        }
    }

    private String routingKey(String tipo) {
        if (tipo == null) {
            return "evento.generico";
        }
        return switch (tipo) {
            case "PELICULA_CREADA" -> "pelicula.creada";
            case "TICKET_PAGADO" -> "ticket.pagado";
            case "TICKET_DEVUELTO" -> "ticket.devuelto";
            default -> "evento.generico";
        };
    }
}
