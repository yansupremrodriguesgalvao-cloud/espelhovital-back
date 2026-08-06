package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.model.Consulta;
import com.example.espelhovitalback.service.ConsultaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consultas")
@CrossOrigin("*")
public class ConsultaController {

    @Autowired
    private ConsultaService service;

    @GetMapping
    public List<Consulta> listar(@RequestParam(required = false) Long usuarioId) {
        if (usuarioId != null) {
            return service.listarPorUsuario(usuarioId);
        }
        return service.listarTodas();
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
    public Consulta salvar(@RequestBody Consulta consulta) {
        return service.salvar(consulta);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Consulta consulta) {
        try {
            return ResponseEntity.ok(service.atualizar(id, consulta));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
