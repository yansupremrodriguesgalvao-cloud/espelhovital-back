package com.example.espelhovitalback.config;


import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private record Window(long start, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String path = req.getServletPath();
        if (!("POST".equals(req.getMethod()) || "DELETE".equals(req.getMethod())) || !(path.equals("/api/login") || path.equals("/usuarios/face/login")
                || path.equals("/usuarios/cadastrar") || path.startsWith("/api/senha/") || path.equals("/grok") || path.startsWith("/conta"))) {
            chain.doFilter(req, res);
            return;
        }
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue().start() > 60000);
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String identity = (path.equals("/grok") || path.startsWith("/conta")) && auth != null && auth.isAuthenticated() ? auth.getName() : req.getRemoteAddr();
        String key = identity + ":" + path;
        if (windows.size() >= 10000 && !windows.containsKey(key)) {
            res.setStatus(429);
            return;
        }
        Window window = windows.compute(key, (k, w) -> w == null || now - w.start() > 60000 ? new Window(now, 1) : new Window(w.start(), w.count() + 1));
        if (window.count() > 10) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"mensagem\":\"Muitas tentativas. Aguarde um minuto.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
