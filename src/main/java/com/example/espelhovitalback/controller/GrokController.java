package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.service.GrokService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/grok")
@CrossOrigin("*")
public class GrokController {

    private final GrokService grokService;

    public GrokController(GrokService grokService) {
        this.grokService = grokService;
    }

    @PostMapping
    public Map<String, String> conversar(@RequestBody Map<String, String> request) {

        String mensagem = request.get("mensagem");

        if (mensagem == null || mensagem.isBlank()) {
            return Map.of(
                    "erro", "A mensagem não pode estar vazia."
            );
        }

        String resposta = grokService.conversar(mensagem);

        return Map.of(
                "resposta", resposta
        );
    }
}