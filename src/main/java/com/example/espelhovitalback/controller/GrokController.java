package com.example.espelhovitalback.controller;


import com.example.espelhovitalback.service.GrokService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/grok")
public class GrokController {
    public record Message(@NotBlank @Size(max = 4000) String mensagem) {
    }

    private final GrokService service;

    public GrokController(GrokService service) {
        this.service = service;
    }

    @PostMapping
    public Map<String, String> conversar(@Valid @RequestBody Message body) {
        return Map.of("resposta", service.conversar(body.mensagem()));
    }
}
