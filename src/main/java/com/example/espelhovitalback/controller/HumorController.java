package com.example.espelhovitalback.controller;


import com.example.espelhovitalback.model.Humor;
import com.example.espelhovitalback.service.HumorService;
import jakarta.validation.Valid;
import com.example.espelhovitalback.dto.HumorRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/humor")
public class HumorController {
    private final HumorService service;

    public HumorController(HumorService service) {
        this.service = service;
    }

    @GetMapping("/historico")
    public com.example.espelhovitalback.dto.HistoryPage<Humor> historico(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return service.historico(pagina, tamanho);
    }

    @GetMapping
    public List<Humor> listar() {
        return service.listar();
    }

    @PostMapping
    public Humor salvar(@Valid @RequestBody HumorRequest body) {
        return service.salvar(body.toEntity());
    }
}
