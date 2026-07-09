package com.portalcomunitario.mscommunity.agrupacion;

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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgrupacionControllerTest {

    @Mock private AgrupacionService service;

    private AgrupacionController controller;

    @BeforeEach
    void setUp() {
        controller = new AgrupacionController(service);
    }

    private JwtAuthenticationToken auth(String email, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(email).claim("role", role).build();
        return new JwtAuthenticationToken(jwt);
    }

    private AgrupacionRequest req() {
        return new AgrupacionRequest("Taller", "d", "r", 3, "18:00", null, null);
    }

    @Test
    @DisplayName("findAll: delega con el email del token")
    void findAll_delega() {
        when(service.findAll("v@x.com")).thenReturn(List.of());
        controller.findAll(auth("v@x.com", "VECINO"));
        verify(service).findAll("v@x.com");
    }

    @Test
    @DisplayName("misInscripciones: delega con el email del token")
    void misInscripciones_delega() {
        when(service.misInscripciones("v@x.com")).thenReturn(List.of());
        controller.misInscripciones(auth("v@x.com", "VECINO"));
        verify(service).misInscripciones("v@x.com");
    }

    @Test
    @DisplayName("create: un vecino recibe 403")
    void create_vecino_403() {
        assertThatThrownBy(() -> controller.create(req(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("create: un dirigente delega")
    void create_admin_delega() {
        AgrupacionRequest r = req();
        controller.create(r, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).create(r);
    }

    @Test
    @DisplayName("update: un dirigente delega con email")
    void update_admin_delega() {
        UUID id = UUID.randomUUID();
        AgrupacionRequest r = req();
        controller.update(id, r, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).update(id, r, "a@x.com");
    }

    @Test
    @DisplayName("delete: un dirigente delega")
    void delete_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.delete(id, auth("a@x.com", "PLATFORM_ADMIN"));
        verify(service).delete(id);
    }

    @Test
    @DisplayName("inscribirse: delega con el email del token")
    void inscribirse_delega() {
        UUID id = UUID.randomUUID();
        controller.inscribirse(id, auth("v@x.com", "VECINO"));
        verify(service).inscribirse(id, "v@x.com");
    }

    @Test
    @DisplayName("salir: delega con el email del token")
    void salir_delega() {
        UUID id = UUID.randomUUID();
        controller.salir(id, auth("v@x.com", "VECINO"));
        verify(service).salir(id, "v@x.com");
    }

    @Test
    @DisplayName("socios: un vecino recibe 403")
    void socios_vecino_403() {
        assertThatThrownBy(() -> controller.socios(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("cancelarReunion: dirigente con fecha válida delega parseando la fecha")
    void cancelarReunion_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.cancelarReunion(id, Map.of("fecha", "2026-07-15"), auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).cancelarReunion(id, LocalDate.of(2026, 7, 15));
    }

    @Test
    @DisplayName("cancelarReunion: sin fecha en el body lanza 400")
    void cancelarReunion_sinFecha_400() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> controller.cancelarReunion(id, Map.of(), auth("a@x.com", "COMMUNITY_ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Fecha");
    }

    @Test
    @DisplayName("reactivarReunion: dirigente delega parseando la fecha")
    void reactivarReunion_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.reactivarReunion(id, "2026-07-15", auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).reactivarReunion(id, LocalDate.of(2026, 7, 15));
    }

    @Test
    @DisplayName("update: un vecino recibe 403")
    void update_vecino_403() {
        assertThatThrownBy(() -> controller.update(UUID.randomUUID(), req(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("delete: un vecino recibe 403")
    void delete_vecino_403() {
        assertThatThrownBy(() -> controller.delete(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("socios: un dirigente delega en el servicio")
    void socios_admin_delega() {
        UUID id = UUID.randomUUID();
        when(service.socios(id)).thenReturn(List.of("v@x.com"));
        controller.socios(id, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).socios(id);
    }

    @Test
    @DisplayName("cancelarReunion: un vecino recibe 403")
    void cancelarReunion_vecino_403() {
        assertThatThrownBy(() -> controller.cancelarReunion(
                UUID.randomUUID(), Map.of("fecha", "2026-07-15"), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("cancelarReunion: fecha en blanco en el body lanza 400")
    void cancelarReunion_fechaBlank_400() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> controller.cancelarReunion(
                id, Map.of("fecha", "   "), auth("a@x.com", "COMMUNITY_ADMIN")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Fecha");
    }

    @Test
    @DisplayName("reactivarReunion: un vecino recibe 403")
    void reactivarReunion_vecino_403() {
        assertThatThrownBy(() -> controller.reactivarReunion(
                UUID.randomUUID(), "2026-07-15", auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("inscribirse: sin subject usa el claim email del token")
    void inscribirse_sinSubject_usaEmailClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("email", "correo@x.com").claim("role", "VECINO").build();
        UUID id = UUID.randomUUID();
        controller.inscribirse(id, new JwtAuthenticationToken(jwt));
        verify(service).inscribirse(id, "correo@x.com");
    }

    @Test
    @DisplayName("findAll: sin autenticación (auth nulo) responde 401")
    void findAll_authNull_401() {
        assertThatThrownBy(() -> controller.findAll(null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("create: token sin claim role es tratado como VECINO y recibe 403")
    void create_sinClaimRole_403() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("x@x.com").build();
        assertThatThrownBy(() -> controller.create(req(), new JwtAuthenticationToken(jwt)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("create: Authentication no-JWT es tratado como VECINO y recibe 403")
    void create_authNoJwt_403() {
        Authentication a = mock(Authentication.class);
        assertThatThrownBy(() -> controller.create(req(), a))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("findAll: sin subject ni email usa el claim name del token")
    void findAll_usaNameClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("name", "nombre@x.com").claim("role", "VECINO").build();
        when(service.findAll("nombre@x.com")).thenReturn(List.of());
        controller.findAll(new JwtAuthenticationToken(jwt));
        verify(service).findAll("nombre@x.com");
    }

    @Test
    @DisplayName("findAll: Authentication no-JWT usa getName() como email")
    void findAll_authNoJwt_usaGetName() {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn("plano@x.com");
        when(service.findAll("plano@x.com")).thenReturn(List.of());
        controller.findAll(a);
        verify(service).findAll("plano@x.com");
    }

}
