package com.portalcomunitario.mscommunity.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Cubre el productor de eventos de ms-community: envío correcto al exchange y
 * la resiliencia cuando el broker está caído (no propaga la excepción).
 */
class NotificacionPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final NotificacionPublisher publisher = new NotificacionPublisher(rabbitTemplate);

    private NotificacionEvento evento() {
        return new NotificacionEvento("EVENTO_COMUNIDAD", "Título", "Mensaje",
                List.of(new Destinatario("Juan", "juan@example.com", "+56912345678", true)));
    }

    @Test
    @DisplayName("publicar: envía al exchange portal.events con la routing key recibida")
    void publicar_enviaAlExchangeConLaRoutingKey() {
        publisher.publicar(RabbitConfig.RK_EVENTO_COMUNIDAD, evento());

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.RK_EVENTO_COMUNIDAD), any(Object.class));
    }

    @Test
    @DisplayName("publicar: con lista de destinatarios nula igual publica (rama n=0)")
    void publicar_destinatariosNulos_publicaSinFallar() {
        NotificacionEvento sinDestinatarios =
                new NotificacionEvento("CUOTA_PENDIENTE", "Título", "Mensaje", null);

        assertThatCode(() -> publisher.publicar(RabbitConfig.RK_CUOTA_PENDIENTE, sinDestinatarios))
                .doesNotThrowAnyException();

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.RK_CUOTA_PENDIENTE), any(Object.class));
    }

    @Test
    @DisplayName("publicar: si el broker lanza excepción, la captura y no la propaga")
    void publicar_siElBrokerFalla_noPropagaLaExcepcion() {
        doThrow(new RuntimeException("broker caído"))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        assertThatCode(() -> publisher.publicar(RabbitConfig.RK_EVENTO_RECORDATORIO, evento()))
                .doesNotThrowAnyException();
    }
}
