package com.example.espelhovitalback.config;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    public Long id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new AccessDeniedException("Autenticação necessária.");
        return Long.valueOf(auth.getName());
    }

    public void requireOwner(Long id) {
        if (!id().equals(id)) throw new AccessDeniedException("Acesso não permitido.");
    }
}
