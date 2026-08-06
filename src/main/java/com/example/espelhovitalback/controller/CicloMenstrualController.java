package com.example.espelhovitalback.controller;

import com.example.espelhovitalback.dto.CicloRequest;
import com.example.espelhovitalback.dto.CicloResponse;
import com.example.espelhovitalback.model.CicloMenstrual;
import com.example.espelhovitalback.repository.CicloMenstrualRepository;
import com.example.espelhovitalback.service.CicloMenstrualService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ciclo")
@CrossOrigin("*")
public class CicloMenstrualController {

    @Autowired
    private CicloMenstrualService service;

    @Autowired
    private CicloMenstrualRepository repository;

    @PostMapping("/calcular")
    public CicloResponse calcular(
            @RequestBody CicloRequest request){

        return service.calcularCiclo(request);

    }

    /** Consulta o histórico de ciclos já calculados/salvos. */
    @GetMapping
    public List<CicloMenstrual> listar() {
        return repository.findAll();
    }
}