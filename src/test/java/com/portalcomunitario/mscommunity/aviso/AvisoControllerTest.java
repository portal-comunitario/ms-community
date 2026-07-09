package com.portalcomunitario.mscommunity.aviso;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisoControllerTest {

    @Mock private AvisoService service;

    private AvisoController controller;

    @BeforeEach
    void setUp() {
        controller = new AvisoController(service);
    }

    private JwtAuthenticationToken auth(String email, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(email).claim("role", role).build();
        return new JwtAuthenticationToken(jwt);
    }

    private AvisoRequest req() {
        return new AvisoRequest("t", "d", "servicio", null, null, null, null, null);
    }

    @Test
    @DisplayName("findAll: delega pasando el rol del token")
    void findAll_delega() {
        when(service.findAll("VECINO")).thenReturn(List.of());
        controller.findAll(auth("v@x.com", "VECINO"));
        verify(service).findAll("VECINO");
    }

    @Test
    @DisplayName("findPendientes: un vecino recibe 403")
    void findPendientes_vecino_403() {
        assertThatThrownBy(() -> controller.findPendientes(auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("findPendientes: un dirigente delega")
    void findPendientes_admin_delega() {
        when(service.findPendientes()).thenReturn(List.of());
        controller.findPendientes(auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).findPendientes();
    }

    @Test
    @DisplayName("create: pasa el email del token como autor")
    void create_delegaConEmail() {
        AvisoRequest r = req();
        controller.create(r, auth("autor@x.com", "VECINO"));
        verify(service).create(r, "autor@x.com");
    }

    @Test
    @DisplayName("update: delega con email y rol del token")
    void update_delega() {
        UUID id = UUID.randomUUID();
        AvisoRequest r = req();
        controller.update(id, r, auth("autor@x.com", "VECINO"));
        verify(service).update(id, r, "autor@x.com", "VECINO");
    }

    @Test
    @DisplayName("aprobar: un vecino recibe 403")
    void aprobar_vecino_403() {
        assertThatThrownBy(() -> controller.aprobar(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("rechazar: un dirigente delega")
    void rechazar_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.rechazar(id, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).rechazar(id);
    }

    @Test
    @DisplayName("marcarResuelto: delega con email y rol")
    void marcarResuelto_delega() {
        UUID id = UUID.randomUUID();
        controller.marcarResuelto(id, auth("autor@x.com", "VECINO"));
        verify(service).marcarResuelto(id, "autor@x.com", "VECINO");
    }

    @Test
    @DisplayName("delete: delega con email y rol")
    void delete_delega() {
        UUID id = UUID.randomUUID();
        controller.delete(id, auth("autor@x.com", "VECINO"));
        verify(service).delete(id, "autor@x.com", "VECINO");
    }

    @Test
    @DisplayName("findById: delega en el servicio")
    void findById_delega() {
        UUID id = UUID.randomUUID();
        controller.findById(id);
        verify(service).findById(id);
    }

    @Test
    @DisplayName("aprobar: un dirigente delega en el servicio")
    void aprobar_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.aprobar(id, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).aprobar(id);
    }

    @Test
    @DisplayName("rechazar: un vecino recibe 403")
    void rechazar_vecino_403() {
        assertThatThrownBy(() -> controller.rechazar(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("create: sin subject usa el claim email como autor")
    void create_sinSubject_usaEmailClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("email", "correo@x.com").claim("role", "VECINO").build();
        AvisoRequest r = req();
        controller.create(r, new JwtAuthenticationToken(jwt));
        verify(service).create(r, "correo@x.com");
    }

    @Test
    @DisplayName("create: sin subject ni email usa el claim name como autor")
    void create_sinSubjectNiEmail_usaNameClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("name", "nombre@x.com").claim("role", "VECINO").build();
        AvisoRequest r = req();
        controller.create(r, new JwtAuthenticationToken(jwt));
        verify(service).create(r, "nombre@x.com");
    }

    @Test
    @DisplayName("create: con Authentication no-JWT usa getName() como autor")
    void create_authNoJwt_usaGetName() {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn("legacy@x.com");
        AvisoRequest r = req();
        controller.create(r, a);
        verify(service).create(r, "legacy@x.com");
    }

    @Test
    @DisplayName("create: sin autenticación (auth nulo) responde 401")
    void create_authNull_401() {
        assertThatThrownBy(() -> controller.create(req(), null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("findAll: token sin claim role trata al usuario como VECINO")
    void findAll_rolNulo_defaultVecino() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("v@x.com").build();
        when(service.findAll("VECINO")).thenReturn(List.of());
        controller.findAll(new JwtAuthenticationToken(jwt));
        verify(service).findAll("VECINO");
    }

}
