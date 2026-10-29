package com.example.espelhovitalback.controller;


import com.example.espelhovitalback.dto.*;
import com.example.espelhovitalback.service.*;
import com.example.espelhovitalback.config.CurrentUser;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
public class LoginController {
    private final UsuarioService users;
    private final SessionService sessions;
    private final CurrentUser current;

    public LoginController(UsuarioService users, SessionService sessions, CurrentUser current) {
        this.users = users;
        this.sessions = sessions;
        this.current = current;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @GetMapping("/sessao")
    public PerfilDTO session() {
        return users.buscarPerfil(current.id());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest req, HttpServletResponse res) {
        var user = users.login(body.getEmail(), body.getSenha())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos."));
        return sessions.login(user, req, res);
    }
}
