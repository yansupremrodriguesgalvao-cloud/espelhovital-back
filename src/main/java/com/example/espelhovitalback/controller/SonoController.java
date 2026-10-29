package com.example.espelhovitalback.controller;


import com.example.espelhovitalback.model.Sono;
import com.example.espelhovitalback.service.SonoService;
import jakarta.validation.Valid;
import com.example.espelhovitalback.dto.SonoRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sono")
public class SonoController {
    private final SonoService service;

    public SonoController(SonoService service) {
        this.service = service;
    }

    @GetMapping("/historico")
    public com.example.espelhovitalback.dto.HistoryPage<Sono> historico(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return service.historico(pagina, tamanho);
    }

    @GetMapping
    public List<Sono> listar() {
        return service.listar();
    }

    @PostMapping
    public Sono salvar(@Valid @RequestBody SonoRequest body) {
        return service.salvar(body.toEntity());
    }
}
