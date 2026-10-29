package com.example.espelhovitalback.controller;


import com.example.espelhovitalback.dto.*;
import com.example.espelhovitalback.model.CicloMenstrual;
import com.example.espelhovitalback.service.CicloMenstrualService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/ciclo")
public class CicloMenstrualController {
    private final CicloMenstrualService service;

    public CicloMenstrualController(CicloMenstrualService service) {
        this.service = service;
    }

    @GetMapping
    public List<CicloMenstrual> listar() {
        return service.listar();
    }

    @GetMapping("/calendario")
    public CalendarioResponse calendario() {
        return service.calendario();
    }

    @PostMapping("/calcular")
    public CicloResponse calcular(@Valid @RequestBody CicloRequest request) {
        return service.calcularCiclo(request);
    }

    @PostMapping("/inicio")
    public CicloInicioResponse registrarInicio(@Valid @RequestBody CicloInicioRequest request) {
        return service.registrarInicio(request.data());
    }

    @PostMapping("/{id}/fim")
    public CalendarioResponse registrarFim(@PathVariable Long id, @Valid @RequestBody CicloFimRequest request) {
        return service.registrarFim(id, request.data());
    }

    @PutMapping("/{id}")
    public CicloMenstrual atualizar(@PathVariable Long id, @Valid @RequestBody CicloRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
