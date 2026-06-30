package com.portalcomunitario.mscommunity.post;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public List<PostResponse> findAll(Authentication auth) {
        return postService.findAll(extractRole(auth));
    }

    @GetMapping("/pendientes")
    public List<PostResponse> findPendientes(Authentication auth) {
        requireAdminOrModerador(auth);
        return postService.findPendientes();
    }

    @GetMapping("/{id}")
    public PostResponse findById(@PathVariable UUID id) {
        return postService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@RequestBody PostRequest request, Authentication auth) {
        return postService.create(request, auth.getName());
    }

    @PutMapping("/{id}/aprobar")
    public PostResponse aprobar(@PathVariable UUID id, Authentication auth) {
        requireAdminOrModerador(auth);
        return postService.aprobar(id);
    }

    @PutMapping("/{id}/rechazar")
    public PostResponse rechazar(@PathVariable UUID id, Authentication auth) {
        requireAdminOrModerador(auth);
        return postService.rechazar(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        postService.delete(id);
    }

    private String extractRole(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String role = jwtAuth.getToken().getClaimAsString("role");
            return role != null ? role : "VECINO";
        }
        return "VECINO";
    }

    private String extractTenantId(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString("tenantId");
        }
        return null;
    }

    private void requireAdminOrModerador(Authentication auth) {
        String role = extractRole(auth);
        if (!"COMMUNITY_ADMIN".equals(role) && !"PLATFORM_ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado");
        }
    }
}
