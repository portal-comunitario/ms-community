package com.portalcomunitario.mscommunity.cuota;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaControllerTest {

    @Mock private CuotaService service;

    private CuotaController controller;

    @BeforeEach
    void setUp() {
        controller = new CuotaController(service);
    }

    private JwtAuthenticationToken auth(String email, String role) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(email).claim("role", role).build();
        return new JwtAuthenticationToken(jwt);
    }

    private CuotaPeriodo periodo() {
        CuotaPeriodo p = new CuotaPeriodo();
        p.setMonto(1000);
        p.setPeriodicidad(Periodicidad.MENSUAL);
        p.setEstado(EstadoPeriodo.ABIERTA);
        return p;
    }

    private Cuota cuota() {
        Cuota c = new Cuota();
        c.setEtiqueta("Enero");
        c.setMonto(1000);
        c.setVencimiento(LocalDate.of(2026, 1, 31));
        c.setVecinoEmail("v@x.com");
        return c;
    }

    @Test
    @DisplayName("activar: un vecino recibe 403")
    void activar_vecino_403() {
        CuotaRequest req = new CuotaRequest(1000, "MENSUAL", LocalDate.now(), LocalDate.now().plusMonths(1));
        assertThatThrownBy(() -> controller.activar(UUID.randomUUID(), req, auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("activar: un dirigente delega y arma el DTO del período")
    void activar_admin_delega() {
        UUID id = UUID.randomUUID();
        CuotaRequest req = new CuotaRequest(1000, "MENSUAL", LocalDate.now(), LocalDate.now().plusMonths(1));
        when(service.activar(id, req)).thenReturn(periodo());

        CuotaPeriodoDto dto = controller.activar(id, req, auth("a@x.com", "COMMUNITY_ADMIN"));

        assertThat(dto.monto()).isEqualTo(1000);
        assertThat(dto.periodicidad()).isEqualTo("MENSUAL");
        verify(service).activar(id, req);
    }

    @Test
    @DisplayName("cerrar: un dirigente delega")
    void cerrar_admin_delega() {
        UUID id = UUID.randomUUID();
        when(service.cerrar(id)).thenReturn(periodo());
        controller.cerrar(id, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).cerrar(id);
    }

    @Test
    @DisplayName("actualizarMonto: un dirigente delega con el monto del body")
    void actualizarMonto_admin_delega() {
        UUID id = UUID.randomUUID();
        when(service.actualizarMonto(id, 2000)).thenReturn(periodo());
        controller.actualizarMonto(id, new CuotaController.MontoRequest(2000), auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).actualizarMonto(id, 2000);
    }

    @Test
    @DisplayName("periodo: devuelve el DTO cuando hay período; null cuando no")
    void periodo_devuelveODTOoNull() {
        UUID id = UUID.randomUUID();
        when(service.periodoActivo(id)).thenReturn(periodo());
        assertThat(controller.periodo(id)).isNotNull();

        UUID id2 = UUID.randomUUID();
        when(service.periodoActivo(id2)).thenReturn(null);
        assertThat(controller.periodo(id2)).isNull();
    }

    @Test
    @DisplayName("todas: un dirigente obtiene la lista mapeada a DTO")
    void todas_admin_mapea() {
        UUID id = UUID.randomUUID();
        when(service.deAgrupacion(id)).thenReturn(List.of(cuota()));
        assertThat(controller.todas(id, auth("a@x.com", "COMMUNITY_ADMIN"))).hasSize(1);
    }

    @Test
    @DisplayName("todas: un vecino recibe 403")
    void todas_vecino_403() {
        assertThatThrownBy(() -> controller.todas(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("mias: delega usando el email del token")
    void mias_delega() {
        UUID id = UUID.randomUUID();
        when(service.misCuotas(id, "v@x.com")).thenReturn(List.of(cuota()));
        assertThat(controller.mias(id, auth("v@x.com", "VECINO"))).hasSize(1);
        verify(service).misCuotas(id, "v@x.com");
    }

    @Test
    @DisplayName("pagar: un dirigente marca la cuota como pagada")
    void pagar_admin_delega() {
        UUID cid = UUID.randomUUID();
        when(service.marcarPago(cid, true)).thenReturn(cuota());
        controller.pagar(cid, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).marcarPago(cid, true);
    }

    @Test
    @DisplayName("pendiente: un dirigente revierte el pago")
    void pendiente_admin_delega() {
        UUID cid = UUID.randomUUID();
        when(service.marcarPago(cid, false)).thenReturn(cuota());
        controller.pendiente(cid, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).marcarPago(cid, false);
    }

    @Test
    @DisplayName("cerrar: un vecino recibe 403")
    void cerrar_vecino_403() {
        assertThatThrownBy(() -> controller.cerrar(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("actualizarMonto: un vecino recibe 403")
    void actualizarMonto_vecino_403() {
        assertThatThrownBy(() -> controller.actualizarMonto(
                UUID.randomUUID(), new CuotaController.MontoRequest(2000), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("actualizarMonto: body nulo delega con monto null")
    void actualizarMonto_bodyNull_delegaConNull() {
        UUID id = UUID.randomUUID();
        when(service.actualizarMonto(id, null)).thenReturn(periodo());
        controller.actualizarMonto(id, null, auth("a@x.com", "COMMUNITY_ADMIN"));
        verify(service).actualizarMonto(id, null);
    }

    @Test
    @DisplayName("pagar: un vecino recibe 403")
    void pagar_vecino_403() {
        assertThatThrownBy(() -> controller.pagar(UUID.randomUUID(), auth("v@x.com", "VECINO")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("mias: sin subject usa el claim email del token")
    void mias_sinSubject_usaEmailClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("email", "correo@x.com").claim("role", "VECINO").build();
        UUID id = UUID.randomUUID();
        when(service.misCuotas(id, "correo@x.com")).thenReturn(List.of());
        controller.mias(id, new JwtAuthenticationToken(jwt));
        verify(service).misCuotas(id, "correo@x.com");
    }

    @Test
    @DisplayName("cerrar: token sin claim role es tratado como VECINO y recibe 403")
    void cerrar_sinClaimRole_403() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("x@x.com").build();
        assertThatThrownBy(() -> controller.cerrar(UUID.randomUUID(), new JwtAuthenticationToken(jwt)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("cerrar: Authentication no-JWT es tratado como VECINO y recibe 403")
    void cerrar_authNoJwt_403() {
        Authentication a = mock(Authentication.class);
        assertThatThrownBy(() -> controller.cerrar(UUID.randomUUID(), a))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("mias: sin subject ni email usa el claim name del token")
    void mias_usaNameClaim() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("name", "nombre@x.com").claim("role", "VECINO").build();
        UUID id = UUID.randomUUID();
        when(service.misCuotas(id, "nombre@x.com")).thenReturn(List.of());
        controller.mias(id, new JwtAuthenticationToken(jwt));
        verify(service).misCuotas(id, "nombre@x.com");
    }

    @Test
    @DisplayName("mias: Authentication no-JWT usa getName() como email")
    void mias_authNoJwt_usaGetName() {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn("plano@x.com");
        UUID id = UUID.randomUUID();
        when(service.misCuotas(id, "plano@x.com")).thenReturn(List.of());
        controller.mias(id, a);
        verify(service).misCuotas(id, "plano@x.com");
    }

    @Test
    @DisplayName("mias: Authentication no-JWT sin nombre responde 401")
    void mias_authNoJwt_sinNombre_401() {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn(null);
        assertThatThrownBy(() -> controller.mias(UUID.randomUUID(), a))
                .isInstanceOf(ResponseStatusException.class);
    }

}
