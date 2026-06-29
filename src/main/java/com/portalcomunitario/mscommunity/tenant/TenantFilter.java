package com.portalcomunitario.mscommunity.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT = "public";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            TenantContext.setCurrentTenant(resolveTenant(request));
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveTenant(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }

        String subdomain = resolveSubdomain(request.getServerName());
        if (StringUtils.hasText(subdomain)) {
            return subdomain;
        }

        return DEFAULT_TENANT;
    }

    private String resolveSubdomain(String host) {
        if (!StringUtils.hasText(host)) {
            return null;
        }
        // Sin subdominio si es un host plano (localhost) o una IP.
        String[] parts = host.split("\\.");
        if (parts.length < 3) {
            return null;
        }
        String candidate = parts[0];
        if ("www".equalsIgnoreCase(candidate)) {
            return null;
        }
        return candidate;
    }
}
