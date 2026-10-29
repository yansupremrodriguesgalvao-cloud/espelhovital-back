package com.example.espelhovitalback.controller;


import com.example.espelhovitalback.dto.*;
import com.example.espelhovitalback.service.*;
import com.example.espelhovitalback.config.CurrentUser;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService service;
    private final CurrentUser current;
    private final SessionService sessions;

    public UsuarioController(UsuarioService service, CurrentUser current, SessionService sessions) {
        this.service = service;
        this.current = current;
        this.sessions = sessions;
    }

    @PostMapping("/cadastrar")
    public Map<String, String> cadastrar(@Valid @RequestBody UsuarioDTO body) {
        service.cadastrar(body);
        return Map.of("mensagem", "Usuária cadastrada com sucesso!");
    }

    @GetMapping("/{id}")
    public PerfilDTO perfil(@PathVariable Long id) {
        return service.buscarPerfil(id);
    }

    @PutMapping("/{id}")
    public PerfilDTO atualizar(@PathVariable Long id, @Valid @RequestBody PerfilDTO body) {
        return service.atualizarPerfil(id, body);
    }

    @PostMapping("/face/cadastrar")
    public Map<String, String> face(@Valid @RequestBody FaceDescriptorRequest body) {
        PerfilDTO profile = service.buscarPerfil(current.id());
        service.login(profile.getEmail(), body.getSenha()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Senha inválida."));
        service.cadastrarDescritorFacial(current.id(), body.getDescritor());
        return Map.of("mensagem", "Rosto cadastrado com sucesso.");
    }

    @DeleteMapping("/face")
    public ResponseEntity<Void> deleteFace() {
        service.removerFace();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/face/login")
    public LoginResponse faceLogin(@Valid @RequestBody FaceDescriptorRequest body, HttpServletRequest req, HttpServletResponse res) {
        if (body.getEmail() == null || body.getEmail().isBlank())
            throw new IllegalArgumentException("Informe e-mail e senha para confirmar sua identidade.");
        var user = service.loginPorReconhecimentoFacial(body.getEmail(), body.getSenha(), body.getDescritor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível confirmar a identidade. Use e-mail e senha."));
        return sessions.login(user, req, res);
    }
}
