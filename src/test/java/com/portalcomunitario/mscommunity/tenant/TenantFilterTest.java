package com.portalcomunitario.mscommunity.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    /** Captura el tenant vigente DURANTE la ejecución de la cadena. */
    private String tenantDuranteCadena(HttpServletRequest req) throws Exception {
        HttpServletResponse res = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        String[] holder = new String[1];
        doAnswer(inv -> { holder[0] = TenantContext.getCurrentTenant(); return null; })
                .when(chain).doFilter(req, res);
        filter.doFilterInternal(req, res, chain);
        return holder[0];
    }

    @Test
    @DisplayName("usa el header X-Tenant-ID cuando está presente y limpia al terminar")
    void usaHeaderXTenantId() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-ID")).thenReturn(" villa_el_sol ");

        assertThat(tenantDuranteCadena(req)).isEqualTo("villa_el_sol");
        assertThat(TenantContext.getCurrentTenant()).isNull(); // se limpia al terminar
    }

    @Test
    @DisplayName("usa el subdominio cuando no hay header")
    void usaSubdominio() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-ID")).thenReturn(null);
        when(req.getServerName()).thenReturn("villa.portal.cl");

        assertThat(tenantDuranteCadena(req)).isEqualTo("villa");
    }

    @Test
    @DisplayName("ignora el subdominio 'www' y cae a public")
    void ignoraWww() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-ID")).thenReturn(null);
        when(req.getServerName()).thenReturn("www.portal.cl");

        assertThat(tenantDuranteCadena(req)).isEqualTo("public");
    }

    @Test
    @DisplayName("cae a public con host plano (localhost) sin header ni subdominio")
    void caeAPublicEnLocalhost() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-ID")).thenReturn(null);
        when(req.getServerName()).thenReturn("localhost");

        assertThat(tenantDuranteCadena(req)).isEqualTo("public");
    }

    @Test
    @DisplayName("cae a public cuando el host es nulo")
    void caeAPublicConHostNulo() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Tenant-ID")).thenReturn(null);
        when(req.getServerName()).thenReturn(null);

        assertThat(tenantDuranteCadena(req)).isEqualTo("public");
    }
}
