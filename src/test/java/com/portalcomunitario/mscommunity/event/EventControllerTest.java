package com.portalcomunitario.mscommunity.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock private EventService eventService;

    private EventController controller;

    @BeforeEach
    void setUp() {
        controller = new EventController(eventService);
    }

    private JwtAuthenticationToken auth(String email, String nombre, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(email).claim("name", nombre).claim("role", role).build();
        return new JwtAuthenticationToken(jwt);
    }

    private EventRequest req() {
        return new EventRequest("t", "d", LocalDateTime.now(), null, "Plaza",
                "GENERAL", null, null, null, null, null, false, null, null, null);
    }

    @Test
    @DisplayName("findAll: delega en el servicio")
    void findAll_delega() {
        when(eventService.findAll()).thenReturn(List.of());
        controller.findAll();
        verify(eventService).findAll();
    }

    @Test
    @DisplayName("findById: delega en el servicio")
    void findById_delega() {
        UUID id = UUID.randomUUID();
        controller.findById(id);
        verify(eventService).findById(id);
    }

    @Test
    @DisplayName("create: un vecino recibe 403")
    void create_vecino_403() {
        assertThatThrownBy(() -> controller.create(req(), auth("v@x.com", "Vecino", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("create: un dirigente delega con email y nombre del token")
    void create_admin_delega() {
        EventRequest r = req();
        controller.create(r, auth("a@x.com", "Ana Dirigente", "COMMUNITY_ADMIN"));
        verify(eventService).create(r, "a@x.com", "Ana Dirigente");
    }

    @Test
    @DisplayName("update: un dirigente delega")
    void update_admin_delega() {
        UUID id = UUID.randomUUID();
        EventRequest r = req();
        controller.update(id, r, auth("a@x.com", "Ana", "PLATFORM_ADMIN"));
        verify(eventService).update(id, r);
    }

    @Test
    @DisplayName("notificar: un dirigente delega la difusión")
    void notificar_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.notificar(id, auth("a@x.com", "Ana", "COMMUNITY_ADMIN"));
        verify(eventService).notificarComunidad(id);
    }

    @Test
    @DisplayName("notificar: un vecino recibe 403")
    void notificar_vecino_403() {
        assertThatThrownBy(() -> controller.notificar(UUID.randomUUID(), auth("v@x.com", "V", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("delete: un dirigente delega")
    void delete_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.delete(id, auth("a@x.com", "Ana", "COMMUNITY_ADMIN"));
        verify(eventService).delete(id);
    }

    @Test
    @DisplayName("update: un vecino recibe 403")
    void update_vecino_403() {
        assertThatThrownBy(() -> controller.update(UUID.randomUUID(), req(), auth("v@x.com", "V", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("delete: un vecino recibe 403")
    void delete_vecino_403() {
        assertThatThrownBy(() -> controller.delete(UUID.randomUUID(), auth("v@x.com", "V", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("create: dirigente sin claim name delega con nombre null")
    void create_admin_sinNombre_delegaNombreNull() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject("a@x.com").claim("role", "COMMUNITY_ADMIN").build();
        EventRequest r = req();
        controller.create(r, new JwtAuthenticationToken(jwt));
        verify(eventService).create(r, "a@x.com", null);
    }

    @Test
    @DisplayName("create: con Authentication no-JWT el rol es VECINO y recibe 403")
    void create_authNoJwt_403() {
        Authentication a = mock(Authentication.class);
        assertThatThrownBy(() -> controller.create(req(), a))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("create: dirigente con claim name en blanco delega con nombre null")
    void create_admin_nombreBlank_delegaNombreNull() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject("a@x.com").claim("name", "   ").claim("role", "COMMUNITY_ADMIN").build();
        EventRequest r = req();
        controller.create(r, new JwtAuthenticationToken(jwt));
        verify(eventService).create(r, "a@x.com", null);
    }

}
