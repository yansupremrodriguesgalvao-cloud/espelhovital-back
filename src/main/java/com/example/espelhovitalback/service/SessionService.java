package com.example.espelhovitalback.service;


import com.example.espelhovitalback.model.Usuario;
import com.example.espelhovitalback.dto.LoginResponse;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;

@Service
public class SessionService {
    private final SecurityContextRepository repository;

    public SessionService(SecurityContextRepository repository) {
        this.repository = repository;
    }

    public LoginResponse login(Usuario user, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) request.changeSessionId();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user.getId().toString(), null, List.of()));
        SecurityContextHolder.setContext(context);
        request.getSession().setAttribute("authVersion", user.getAuthVersion());
        repository.saveContext(context, request, response);
        return new LoginResponse(user.getId(), user.getNome(), user.getEmail(), "Login realizado com sucesso!");
    }
}
