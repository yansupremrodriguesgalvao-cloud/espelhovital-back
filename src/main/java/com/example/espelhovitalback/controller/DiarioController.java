package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.dto.*;
import com.example.espelhovitalback.service.DiarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

@RestController
@RequestMapping("/diario")
public class DiarioController {
    private final DiarioService service;

    public DiarioController(DiarioService service) {
        this.service = service;
    }

    @GetMapping
    public HistoryPage<DiarioResponse> history(@RequestParam LocalDate inicio, @RequestParam LocalDate fim,
                                               @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return service.history(inicio, fim, pagina, tamanho);
    }

    @GetMapping("/{data}")
    public DiarioResponse get(@PathVariable LocalDate data) {
        return service.get(data);
    }

    @PutMapping("/{data}")
    public DiarioResponse save(@PathVariable LocalDate data, @Valid @RequestBody DiarioRequest body) {
        return service.save(data, body);
    }

    @DeleteMapping("/{data}")
    public ResponseEntity<Void> delete(@PathVariable LocalDate data) {
        service.delete(data);
        return ResponseEntity.noContent().build();
    }
}
