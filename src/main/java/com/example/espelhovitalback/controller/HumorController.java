package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.model.Humor;
import com.example.espelhovitalback.repository.HumorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/humor")
@CrossOrigin("*")
public class HumorController {

    private final HumorRepository repository;

    public HumorController(HumorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Humor> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Humor salvar(@RequestBody Humor humor) {
        return repository.save(humor);
    }
}