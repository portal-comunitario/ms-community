package com.portalcomunitario.mscommunity.contacto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Cliente hacia ms-auth para resolver contacto + consentimiento de los destinatarios. */
@Component
public class ContactoClient {

    private static final Logger log = LoggerFactory.getLogger(ContactoClient.class);

    private final RestClient http = RestClient.create();
    private final String baseUrl;
    private final String token;

    public ContactoClient(@Value("${app.auth.base-url:http://localhost:8081}") String baseUrl,
                          @Value("${app.internal.token:portal-internal-2026}") String token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    /** Contacto de un email; null si no se encuentra o ms-auth no responde. */
    public Contacto porEmail(String email) {
        List<Contacto> l = porEmails(List.of(email));
        return l.isEmpty() ? null : l.get(0);
    }

    /** Contactos de una lista de emails. */
    public List<Contacto> porEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        return consultar("/auth/contactos?emails=" + String.join(",", emails));
    }

    /** Todos los contactos (broadcast a la comunidad). */
    public List<Contacto> todos() {
        return consultar("/auth/contactos");
    }

    private List<Contacto> consultar(String path) {
        try {
            Contacto[] arr = http.get()
                    .uri(baseUrl + path)
                    .header("X-Internal-Token", token)
                    .retrieve()
                    .body(Contacto[].class);
            return arr != null ? List.of(arr) : List.of();
        } catch (Exception ex) {
            log.error("No se pudo resolver contactos desde ms-auth ({}): {}", path, ex.getMessage());
            return List.of();
        }
    }

    public record Contacto(String email, String nombre, String telefono, boolean notificacionesActivas) {
    }
}
