package com.pasalapeli.movie.messaging;

import com.pasalapeli.movie.event.EventoNotificacion;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica eventos de notificacion hacia AWS SNS de forma desacoplada de la
 * logica de negocio. El envio real ocurre AFTER_COMMIT para no notificar por
 * transacciones que finalmente revierten, y los errores del broker no rompen
 * la operacion de negocio (best-effort).
 *
 * El atributo de mensaje "tipo" alimenta el FilterPolicy de la suscripcion
 * SNS->SQS definida en scripts/setup-messaging.sh.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacionEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final SnsTemplate snsTemplate;

    @Value("${app.sns.topic-arn:}")
    private String topicArn;

    /** Encola el evento para publicarlo cuando la transaccion confirme. */
    public void publicar(EventoNotificacion evento) {
        applicationEventPublisher.publishEvent(evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void enviar(EventoNotificacion evento) {
        try {
            snsTemplate.send(topicArn, MessageBuilder.withPayload(evento)
                    .setHeader("tipo", evento.getTipo())
                    .build());
            log.info("Evento publicado en SNS: {} (topic={})", evento.getTipo(), topicArn);
        } catch (Exception ex) {
            // La mensajeria es best-effort: no debe tumbar la transaccion de negocio.
            log.error("No se pudo publicar el evento {} en SNS: {}", evento.getTipo(), ex.getMessage());
        }
    }
}
