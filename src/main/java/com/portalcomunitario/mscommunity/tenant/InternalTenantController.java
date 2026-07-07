package com.portalcomunitario.mscommunity.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Endpoint interno (servicio-a-servicio) para provisionar el schema de un tenant. */
@RestController
@RequestMapping("/internal/tenants")
public class InternalTenantController {

    private final TenantProvisioner provisioner;

    @Value("${app.internal.token:portal-internal}")
    private String internalToken;

    public InternalTenantController(TenantProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    @PostMapping("/{schema}/provision")
    public Map<String, String> provision(@PathVariable String schema,
                                         @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requireInternal(token);
        provisioner.provision(schema);
        return Map.of("status", "ok", "schema", schema, "service", "community");
    }

    private void requireInternal(String token) {
        if (internalToken == null || !internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token interno inválido");
        }
    }
}
