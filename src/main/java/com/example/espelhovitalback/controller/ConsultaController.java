package com.example.espelhovitalback.controller;


import com.example.espelhovitalback.model.Consulta;
import com.example.espelhovitalback.service.ConsultaService;
import jakarta.validation.Valid;
import com.example.espelhovitalback.dto.ConsultaRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.*;

@RestController
@RequestMapping("/consultas")
public class ConsultaController {
    private final ConsultaService service;

    public ConsultaController(ConsultaService service) {
        this.service = service;
    }

    @GetMapping("/historico")
    public com.example.espelhovitalback.dto.HistoryPage<Consulta> historico(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return service.historico(pagina, tamanho);
    }

    @GetMapping
    public List<Consulta> listar() {
        return service.listarTodas();
    }

    @GetMapping("/{id}")
    public Consulta buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/proximas")
    public List<Consulta> proximas() {
        return service.proximos();
    }

    @GetMapping("/lembretes")
    public List<Consulta> lembretes() {
        return service.lembretesAtivos();
    }

    @PostMapping
    public Consulta salvar(@Valid @RequestBody ConsultaRequest body) {
        return service.salvar(body.toEntity());
    }

    @PutMapping("/{id}")
    public Consulta atualizar(@PathVariable Long id, @Valid @RequestBody ConsultaRequest body) {
        return service.atualizar(id, body.toEntity());
    }

    @PatchMapping("/{id}/status")
    public Consulta status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.status(id, body.get("status"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
