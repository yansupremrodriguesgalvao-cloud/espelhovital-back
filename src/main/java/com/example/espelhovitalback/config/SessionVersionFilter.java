package com.example.espelhovitalback.config;


import com.example.espelhovitalback.repository.UsuarioRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Objects;

@Component
public class SessionVersionFilter extends OncePerRequestFilter {
    private final UsuarioRepository users;

    public SessionVersionFilter(UsuarioRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        var session = req.getSession(false);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (session != null && auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            var user = users.findById(Long.valueOf(auth.getName()));
            if (user.isEmpty() || !Objects.equals(session.getAttribute("authVersion"), user.get().getAuthVersion())) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                res.setStatus(401);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"mensagem\":\"Sua sessÃ£o expirou. Entre novamente.\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
