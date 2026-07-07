package com.portalcomunitario.mscommunity.messaging;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para ms-community (productor de eventos).
 * Publica en el exchange topic {@code portal.events} con las routing keys de cuotas y eventos.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "portal.events";

    // Routing keys que publica ms-community
    public static final String RK_CUOTA_PENDIENTE = "cuota.pendiente";
    public static final String RK_EVENTO_COMUNIDAD = "evento.comunidad";
    public static final String RK_EVENTO_RECORDATORIO = "evento.recordatorio";

    @Bean
    public TopicExchange portalEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setExchange(EXCHANGE);
        return template;
    }
}
