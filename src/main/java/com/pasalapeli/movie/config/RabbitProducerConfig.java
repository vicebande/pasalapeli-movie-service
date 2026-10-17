package com.pasalapeli.movie.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion del lado productor: declara el exchange central (topic) y el
 * conversor JSON usado por el RabbitTemplate para publicar los eventos.
 */
@Configuration
public class RabbitProducerConfig {

    @Value("${notificaciones.exchange:pasalapeli.events}")
    private String exchangeName;

    @Bean
    public TopicExchange pasalapeliEventsExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
