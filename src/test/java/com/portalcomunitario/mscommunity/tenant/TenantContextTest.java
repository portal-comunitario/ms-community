package com.portalcomunitario.mscommunity.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("set/get: guarda y recupera el tenant vigente del hilo")
    void setGet_recuperaTenant() {
        TenantContext.setCurrentTenant("villa_el_sol");
        assertThat(TenantContext.getCurrentTenant()).isEqualTo("villa_el_sol");
    }

    @Test
    @DisplayName("clear: elimina el tenant vigente (vuelve a null)")
    void clear_eliminaTenant() {
        TenantContext.setCurrentTenant("villa_el_sol");
        TenantContext.clear();
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    @DisplayName("get: sin haber fijado nada devuelve null")
    void get_sinFijar_devuelveNull() {
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
