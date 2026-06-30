package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.model.Sono;
import com.example.espelhovitalback.repository.SonoRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sono")
@CrossOrigin("*")
public class SonoController {

    private final SonoRepository repository;

    public SonoController(SonoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Sono> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Sono salvar(@RequestBody Sono sono) {

        System.out.println("RECEBIDO -> horas: " + sono.getHoras());
        System.out.println("RECEBIDO -> qualidade: " + sono.getQualidade());

        return repository.save(sono);
    }
}