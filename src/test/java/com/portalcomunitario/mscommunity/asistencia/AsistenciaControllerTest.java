package com.portalcomunitario.mscommunity.asistencia;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenciaControllerTest {

    @Mock private AsistenciaService service;

    private AsistenciaController controller;

    @BeforeEach
    void setUp() {
        controller = new AsistenciaController(service);
    }

    private JwtAuthenticationToken auth(String email, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(email).claim("role", role).build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    @DisplayName("deActividad: un vecino recibe 403")
    void deActividad_vecino_403() {
        assertThatThrownBy(() -> controller.deActividad(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("deActividad: un dirigente delega")
    void deActividad_admin_delega() {
        UUID id = UUID.randomUUID();
        when(service.deActividad(id)).thenReturn(List.of());
        controller.deActividad(id, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).deActividad(id);
    }

    @Test
    @DisplayName("marcar: guarda la lista y devuelve el estado recalculado")
    void marcar_admin_guardaYRecarga() {
        UUID id = UUID.randomUUID();
        when(service.deActividad(id)).thenReturn(List.of());
        controller.marcar(id, new AsistenciaController.MarcarAsistenciaRequest(List.of("a@x.com")),
                auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).marcar(id, List.of("a@x.com"));
        verify(service).deActividad(id);
    }

    @Test
    @DisplayName("marcar: body nulo se trata como lista vacía")
    void marcar_bodyNulo_listaVacia() {
        UUID id = UUID.randomUUID();
        when(service.deActividad(id)).thenReturn(List.of());
        controller.marcar(id, null, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).marcar(id, List.of());
    }

    @Test
    @DisplayName("miAsistencia: delega usando el email del token")
    void miAsistencia_delega() {
        UUID id = UUID.randomUUID();
        when(service.miAsistencia(id, "v@x.com")).thenReturn(List.of());
        controller.miAsistencia(id, auth("v@x.com", "VECINO"));
        verify(service).miAsistencia(id, "v@x.com");
    }

    @Test
    @DisplayName("marcar: un vecino recibe 403 y no toca el servicio")
    void marcar_vecino_403() {
        assertThatThrownBy(() -> controller.marcar(UUID.randomUUID(),
                new AsistenciaController.MarcarAsistenciaRequest(List.of("a@x.com")),
                auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("deActividad: Authentication no-JWT (VECINO) recibe 403")
    void deActividad_authNoJwt_403() {
        Authentication a = mock(Authentication.class);
        assertThatThrownBy(() -> controller.deActividad(UUID.randomUUID(), a))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("miAsistencia: sin subject usa el claim email del token")
    void miAsistencia_usaEmailClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("email", "correo@x.com").claim("role", "VECINO").build();
        UUID id = UUID.randomUUID();
        when(service.miAsistencia(id, "correo@x.com")).thenReturn(List.of());
        controller.miAsistencia(id, new JwtAuthenticationToken(jwt));
        verify(service).miAsistencia(id, "correo@x.com");
    }

    @Test
    @DisplayName("miAsistencia: Authentication no-JWT usa getName() como email")
    void miAsistencia_authNoJwt_usaGetName() {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn("plano@x.com");
        UUID id = UUID.randomUUID();
        when(service.miAsistencia(id, "plano@x.com")).thenReturn(List.of());
        controller.miAsistencia(id, a);
        verify(service).miAsistencia(id, "plano@x.com");
    }

    @Test
    @DisplayName("miAsistencia: Authentication no-JWT sin nombre responde 401")
    void miAsistencia_authNoJwt_sinNombre_401() {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn(null);
        assertThatThrownBy(() -> controller.miAsistencia(UUID.randomUUID(), a))
                .isInstanceOf(ResponseStatusException.class);
    }
}
