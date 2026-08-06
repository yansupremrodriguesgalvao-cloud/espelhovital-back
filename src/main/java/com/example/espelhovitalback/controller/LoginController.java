package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.dto.LoginRequest;
import com.example.espelhovitalback.dto.LoginResponse;
import com.example.espelhovitalback.model.Usuario;
import com.example.espelhovitalback.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoginController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        Optional<Usuario> usuarioOpt =
                usuarioService.login(request.getEmail(), request.getSenha());

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            return ResponseEntity.ok(new LoginResponse(
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getEmail(),
                    "Login realizado com sucesso!"
            ));
        }

        return ResponseEntity.status(401)
                .body("E-mail ou senha inválidos.");
    }
}
