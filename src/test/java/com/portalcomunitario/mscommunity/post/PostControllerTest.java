package com.portalcomunitario.mscommunity.post;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock private PostService service;

    private PostController controller;

    @BeforeEach
    void setUp() {
        controller = new PostController(service);
    }

    private JwtAuthenticationToken auth(String email, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(email).claim("role", role).build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    @DisplayName("findAll: delega pasando el rol extraído del token")
    void findAll_delegaConRol() {
        when(service.findAll("VECINO")).thenReturn(List.of());

        controller.findAll(auth("v@x.com", "VECINO"));

        verify(service).findAll("VECINO");
    }

    @Test
    @DisplayName("findPendientes: un vecino recibe 403 y no consulta el servicio")
    void findPendientes_vecino_403() {
        assertThatThrownBy(() -> controller.findPendientes(auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("findPendientes: un dirigente delega en el servicio")
    void findPendientes_admin_delega() {
        when(service.findPendientes()).thenReturn(List.of());

        controller.findPendientes(auth("a@x.com", "COMMUNITY_ADMIN"));

        verify(service).findPendientes();
    }

    @Test
    @DisplayName("create: usa el nombre del token como autor")
    void create_usaAutorDelToken() {
        PostRequest req = new PostRequest("t", "c", "noticia", null, null, null);
        controller.create(req, auth("autor@x.com", "VECINO"));

        verify(service).create(req, "autor@x.com");
    }

    @Test
    @DisplayName("aprobar: dirigente delega")
    void aprobar_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.aprobar(id, auth("a@x.com", "PLATFORM_ADMIN"));
        verify(service).aprobar(id);
    }

    @Test
    @DisplayName("rechazar: un vecino recibe 403")
    void rechazar_vecino_403() {
        assertThatThrownBy(() -> controller.rechazar(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("delete: delega sin exigir rol")
    void delete_delega() {
        UUID id = UUID.randomUUID();
        controller.delete(id);
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
    @DisplayName("rechazar: un dirigente delega")
    void rechazar_admin_delega() {
        UUID id = UUID.randomUUID();
        controller.rechazar(id, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).rechazar(id);
    }

    @Test
    @DisplayName("aprobar: un vecino recibe 403 y no consulta el servicio")
    void aprobar_vecino_403() {
        assertThatThrownBy(() -> controller.aprobar(UUID.randomUUID(), auth("v@x.com", "VECINO")))
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

    @Test
    @DisplayName("findAll: Authentication no-JWT es tratado como VECINO")
    void findAll_authNoJwt_defaultVecino() {
        Authentication a = mock(Authentication.class);
        when(service.findAll("VECINO")).thenReturn(List.of());
        controller.findAll(a);
        verify(service).findAll("VECINO");
    }

    @Test
    @DisplayName("findPendientes: Authentication no-JWT (VECINO) recibe 403")
    void findPendientes_authNoJwt_403() {
        Authentication a = mock(Authentication.class);
        assertThatThrownBy(() -> controller.findPendientes(a))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

}
