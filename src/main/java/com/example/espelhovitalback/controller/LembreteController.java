package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.dto.LembreteConfig;
import com.example.espelhovitalback.service.LembreteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/lembretes")
public class LembreteController {
    public record Read(@NotBlank @Pattern(regexp = "[A-Z]+(:[0-9]+)?") @Size(max = 120) String chave) {
    }

    private final LembreteService service;

    public LembreteController(LembreteService service) {
        this.service = service;
    }

    @GetMapping("/config")
    public LembreteConfig get() {
        return service.config();
    }

    @PutMapping("/config")
    public LembreteConfig save(@Valid @RequestBody LembreteConfig.Preferences body) {
        return service.save(body);
    }

    @GetMapping("/hoje")
    public List<LembreteService.Notice> today() {
        return service.today();
    }

    @PostMapping("/lido")
    public ResponseEntity<Void> read(@Valid @RequestBody Read body) {
        service.read(body.chave());
        return ResponseEntity.noContent().build();
    }
}
