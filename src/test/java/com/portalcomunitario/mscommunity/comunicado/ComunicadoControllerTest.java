package com.portalcomunitario.mscommunity.comunicado;

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
class ComunicadoControllerTest {

    @Mock private ComunicadoService service;

    private ComunicadoController controller;

    @BeforeEach
    void setUp() {
        controller = new ComunicadoController(service);
    }

    private JwtAuthenticationToken auth(String email, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(email).claim("role", role).build();
        return new JwtAuthenticationToken(jwt);
    }

    private ComunicadoRequest req() {
        return new ComunicadoRequest("t", "c", "aviso", null);
    }

    @Test
    @DisplayName("findAll: delega en el servicio")
    void findAll_delega() {
        when(service.findAll()).thenReturn(List.of());
        controller.findAll();
        verify(service).findAll();
    }

    @Test
    @DisplayName("create: un vecino recibe 403 y no llega al servicio")
    void create_vecino_403() {
        assertThatThrownBy(() -> controller.create(req(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("create: un dirigente delega usando el email del token")
    void create_admin_delega() {
        ComunicadoRequest r = req();
        controller.create(r, auth("dirigente@x.com", "COMMUNITY_ADMIN"));
        verify(service).create(r, "dirigente@x.com");
    }

    @Test
    @DisplayName("update: un dirigente delega")
    void update_admin_delega() {
        UUID id = UUID.randomUUID();
        ComunicadoRequest r = req();
        controller.update(id, r, auth("a@x.com", "PLATFORM_ADMIN"));
        verify(service).update(id, r);
    }

    @Test
    @DisplayName("delete: un vecino recibe 403")
    void delete_vecino_403() {
        assertThatThrownBy(() -> controller.delete(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("delete: un dirigente delega")
    void delete_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.delete(id, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).delete(id);
    }

    @Test
    @DisplayName("findById: delega en el servicio")
    void findById_delega() {
        UUID id = UUID.randomUUID();
        controller.findById(id);
        verify(service).findById(id);
    }

    @Test
    @DisplayName("update: un vecino recibe 403 y no llega al servicio")
    void update_vecino_403() {
        assertThatThrownBy(() -> controller.update(UUID.randomUUID(), req(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("create: dirigente sin subject usa el claim email como autor")
    void create_admin_sinSubject_usaEmailClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("email", "correo@x.com").claim("role", "COMMUNITY_ADMIN").build();
        ComunicadoRequest r = req();
        controller.create(r, new JwtAuthenticationToken(jwt));
        verify(service).create(r, "correo@x.com");
    }

    @Test
    @DisplayName("create: dirigente sin subject ni email usa el claim name como autor")
    void create_admin_usaNameClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("name", "nombre@x.com").claim("role", "PLATFORM_ADMIN").build();
        ComunicadoRequest r = req();
        controller.create(r, new JwtAuthenticationToken(jwt));
        verify(service).create(r, "nombre@x.com");
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

}
