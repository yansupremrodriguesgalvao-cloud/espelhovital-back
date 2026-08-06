package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.service.RecomendacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recomendacoes")
@CrossOrigin("*")
public class RecomendacaoController {

    @Autowired
    private RecomendacaoService service;

    /** Recomendações personalizadas combinando ciclo, sono e humor. */
    @GetMapping("/personalizadas")
    public Map<String, List<String>> personalizadas() {
        return Map.of("recomendacoes", service.gerarRecomendacoes());
    }
}
