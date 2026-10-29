package com.example.espelhovitalback.controller;


import com.example.espelhovitalback.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/senha")
public class PasswordRecoveryController {
    public record Recovery(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record Reset(@NotBlank @Size(max = 100) String token, @NotBlank @Size(min = 8, max = 72) String senha) {
    }

    private final PasswordRecoveryService service;

    public PasswordRecoveryController(PasswordRecoveryService service) {
        this.service = service;
    }

    @PostMapping("/recuperar")
    public Map<String, String> recover(@Valid @RequestBody Recovery body) {
        service.request(body.email());
        return Map.of("mensagem", "Se o e-mail estiver cadastrado, vocÃª receberÃ¡ instruÃ§Ãµes.");
    }

    @PostMapping("/redefinir")
    public Map<String, String> reset(@Valid @RequestBody Reset body) {
        service.reset(body.token(), body.senha());
        return Map.of("mensagem", "Senha atualizada. Entre novamente.");
    }
}
