package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.service.PrivacyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.Map;

@RestController
@RequestMapping("/conta")
public class PrivacyController {
    public record Export(@NotBlank @Size(max = 72) String senha, boolean incluirBiometria) {
    }

    public record Delete(@NotBlank @Size(max = 72) String senha, @NotBlank @Size(max = 50) String confirmacao) {
    }

    public record Password(@NotBlank @Size(max = 72) String senha) {
    }

    private final PrivacyService service;

    public PrivacyController(PrivacyService service) {
        this.service = service;
    }

    @GetMapping("/privacidade")
    public Map<String, Object> summary() {
        return service.summary();
    }

    @PostMapping("/exportar")
    public ResponseEntity<Map<String, Object>> export(@Valid @RequestBody Export body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=espelho-vital-meus-dados.json").body(service.export(body.senha(), body.incluirBiometria()));
    }

    @DeleteMapping("/dados")
    public ResponseEntity<Void> clear(@Valid @RequestBody Delete body) {
        service.clearHealth(body.senha(), body.confirmacao());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/biometria")
    public ResponseEntity<Void> face(@Valid @RequestBody Password body) {
        service.deleteBiometric(body.senha());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> account(@Valid @RequestBody Delete body, HttpServletRequest request) {
        service.deleteAccount(body.senha(), body.confirmacao());
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
